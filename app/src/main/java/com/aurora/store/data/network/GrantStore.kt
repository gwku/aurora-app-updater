/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.network

import android.util.Base64
import android.util.Log
import com.aurora.Constants
import com.aurora.extensions.TAG
import com.aurora.store.data.model.Grant
import com.aurora.store.data.model.GrantFile
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Reads and writes the grants file hosted in the app's GitHub repository.
 *
 * Reading is anonymous — every device running the app needs it. Writing goes through the GitHub
 * contents API and requires a personal access token, which only the admin has.
 */
@Singleton
class GrantStore @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {

    companion object {
        private const val ACCEPT_JSON = "application/vnd.github+json"
        private const val ACCEPT_RAW = "application/vnd.github.raw"
        private const val API_VERSION = "2022-11-28"

        private const val HTTP_NOT_FOUND = 404
    }

    private val contentsUrl =
        "${Constants.GITHUB_API_URL}/repos/${Constants.GRANTS_REPO_OWNER}/" +
            "${Constants.GRANTS_REPO_NAME}/contents/${Constants.GRANTS_FILE_PATH}"

    private val repoUrl = "${Constants.GITHUB_API_URL}/repos/${Constants.GRANTS_REPO_OWNER}/" +
        Constants.GRANTS_REPO_NAME

    /**
     * Fetches every published grant. An absent grants file simply means nothing has been issued.
     *
     * Falls back to the raw file if the API is unreachable or rate-limited, since a recipient
     * redeeming a code has no token to spend against the higher authenticated limit.
     */
    suspend fun fetchGrants(): List<Grant> = withContext(Dispatchers.IO) {
        try {
            val request = newRequest("$contentsUrl?ref=${Constants.GRANTS_REPO_BRANCH}")
                .header("Accept", ACCEPT_RAW)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                when {
                    response.code == HTTP_NOT_FOUND -> return@withContext emptyList()
                    !response.isSuccessful -> throw IOException("API returned ${response.code}")
                    else -> parseGrants(response.body.string())
                }
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to fetch grants over the API, falling back to raw", exception)
            fetchGrantsFromRaw()
        }
    }

    /**
     * Publishes a grant, replacing any existing grant with the same code hash and dropping the
     * ones that have already expired.
     * @param token GitHub personal access token with write access to the repository contents
     */
    suspend fun publishGrant(token: String, grant: Grant) = withContext(Dispatchers.IO) {
        val (grants, sha) = fetchGrantsForWrite(token)
        val now = System.currentTimeMillis() / 1000
        val updated = grants
            .filterNot { it.isExpired(now) || it.codeHash == grant.codeHash }
            .plus(grant)

        commit(token, updated, sha, "Grant install of ${grant.packageName}")
    }

    /**
     * Withdraws a grant so its code stops working on devices that have not used it yet.
     * @param token GitHub personal access token with write access to the repository contents
     */
    suspend fun revokeGrant(token: String, grant: Grant) = withContext(Dispatchers.IO) {
        val (grants, sha) = fetchGrantsForWrite(token)
        val updated = grants.filterNot { it.codeHash == grant.codeHash }

        commit(token, updated, sha, "Revoke grant for ${grant.packageName}")
    }

    /**
     * Checks that a token can actually reach the repository holding the grants file.
     * @return true if the token is usable
     */
    suspend fun verifyToken(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = newRequest(repoUrl)
                .header("Accept", ACCEPT_JSON)
                .header("Authorization", "Bearer $token")
                .build()

            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to verify admin token", exception)
            false
        }
    }

    private fun newRequest(url: String) = Request.Builder()
        .url(url)
        .header("X-GitHub-Api-Version", API_VERSION)
        .cacheControl(CacheControl.FORCE_NETWORK)

    private fun parseGrants(body: String): List<Grant> = when {
        body.isBlank() -> emptyList()
        else -> json.decodeFromString<GrantFile>(body).grants
    }

    private fun fetchGrantsFromRaw(): List<Grant> {
        val request = newRequest(Constants.GRANTS_URL_RAW).build()

        return okHttpClient.newCall(request).execute().use { response ->
            when {
                response.code == HTTP_NOT_FOUND -> emptyList()
                !response.isSuccessful -> throw IOException("Raw fetch returned ${response.code}")
                else -> parseGrants(response.body.string())
            }
        }
    }

    /**
     * Reads the grants file along with its blob sha, which GitHub requires to overwrite it.
     * @return The current grants, and the sha of the file holding them, null if it does not exist
     */
    private fun fetchGrantsForWrite(token: String): Pair<List<Grant>, String?> {
        val request = newRequest("$contentsUrl?ref=${Constants.GRANTS_REPO_BRANCH}")
            .header("Accept", ACCEPT_JSON)
            .header("Authorization", "Bearer $token")
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            when {
                response.code == HTTP_NOT_FOUND -> emptyList<Grant>() to null
                !response.isSuccessful -> throw IOException("API returned ${response.code}")
                else -> {
                    val payload = JSONObject(response.body.string())
                    val content = payload.optString("content").replace("\n", "")
                    val decoded = String(Base64.decode(content, Base64.DEFAULT))
                    parseGrants(decoded) to payload.getString("sha")
                }
            }
        }
    }

    private fun commit(token: String, grants: List<Grant>, sha: String?, message: String) {
        val contents = json.encodeToString(GrantFile(grants = grants)) + "\n"
        val payload = JSONObject().apply {
            put("message", message)
            put("content", Base64.encodeToString(contents.toByteArray(), Base64.NO_WRAP))
            put("branch", Constants.GRANTS_REPO_BRANCH)
            if (sha != null) put("sha", sha)
        }

        val request = newRequest(contentsUrl)
            .header("Accept", ACCEPT_JSON)
            .header("Authorization", "Bearer $token")
            .put(payload.toString().toRequestBody(Constants.JSON_MIME_TYPE.toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to publish grants, API returned ${response.code}")
            }
        }
    }
}
