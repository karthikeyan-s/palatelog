package com.prismpixelvertex.palatelog.core.sync

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DriveFileItem(
    val id: String,
    val name: String,
    val createdTime: String? = null,
    val modifiedTime: String? = null
)

@Serializable
data class DriveFileListResponse(
    val files: List<DriveFileItem> = emptyList()
)

class GoogleAuthException(
    message: String,
    val userRecoverableIntent: android.content.Intent? = null
) : Exception(message)

@Singleton
class GoogleDriveSyncClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Gets OAuth2 access token for Google Drive API.
     */
    suspend fun getAccessToken(accountEmail: String): String = withContext(Dispatchers.IO) {
        val scope = "oauth2:https://www.googleapis.com/auth/drive.file"
        val account = Account(accountEmail, "com.google")
        try {
            GoogleAuthUtil.getToken(context, account, scope)
        } catch (e: UserRecoverableAuthException) {
            throw GoogleAuthException("Google Auth permission required", e.intent)
        } catch (e: Exception) {
            throw GoogleAuthException("Failed to obtain OAuth token: ${e.localizedMessage}")
        }
    }

    /**
     * Locates or creates the 'PalateLog_Family' folder in Google Drive.
     */
    suspend fun getOrCreateFamilyFolder(accessToken: String): String = withContext(Dispatchers.IO) {
        val folderName = "PalateLog_Family"
        // 1. Search for existing folder
        val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)"

        val request = Request.Builder()
            .url(searchUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Failed to search Drive folder: HTTP ${response.code} - $responseBody")
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(responseBody)
        if (fileList.files.isNotEmpty()) {
            return@withContext fileList.files.first().id
        }

        // 2. Folder does not exist, create it
        val createUrl = "https://www.googleapis.com/drive/v3/files"
        val jsonPayload = """{"name":"$folderName","mimeType":"application/vnd.google-apps.folder"}"""
        val createRequest = Request.Builder()
            .url(createUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        val createResponse = httpClient.newCall(createRequest).execute()
        val createBody = createResponse.body?.string() ?: ""

        if (!createResponse.isSuccessful) {
            throw IOException("Failed to create Drive folder: HTTP ${createResponse.code} - $createBody")
        }

        val createdFolder = json.decodeFromString<DriveFileItem>(createBody)
        createdFolder.id
    }

    /**
     * Uploads a JSON sync payload file to the target Google Drive folder.
     */
    suspend fun uploadSyncPayload(
        accessToken: String,
        folderId: String,
        fileName: String,
        jsonContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        val boundary = "PalateLogSyncBoundary${System.currentTimeMillis()}"

        val multipartBody = StringBuilder().apply {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append("""{"name":"$fileName","parents":["$folderId"]}""")
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(jsonContent)
            append("\r\n--$boundary--\r\n")
        }.toString()

        val request = Request.Builder()
            .url(uploadUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "multipart/related; boundary=$boundary")
            .post(multipartBody.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Failed to upload sync payload: HTTP ${response.code} - $responseBody")
        }

        true
    }

    /**
     * Searches for an existing canonical sync file for this device (changes_<deviceId>.json).
     */
    suspend fun findSyncFileForDevice(
        accessToken: String,
        folderId: String,
        deviceId: String
    ): DriveFileItem? = withContext(Dispatchers.IO) {
        val targetName = "changes_$deviceId.json"
        val query = "'$folderId' in parents and name = '$targetName' and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,createdTime,modifiedTime)&pageSize=1"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return@withContext null
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(responseBody)
        fileList.files.firstOrNull()
    }

    /**
     * Updates an existing Google Drive sync file content in-place via PATCH.
     */
    suspend fun updateSyncPayloadFile(
        accessToken: String,
        fileId: String,
        jsonContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"

        val request = Request.Builder()
            .url(uploadUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json; charset=UTF-8")
            .patch(jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Failed to update sync payload: HTTP ${response.code} - $responseBody")
        }

        true
    }

    /**
     * Uploads or updates the single canonical sync file for this device (changes_<deviceId>.json).
     */
    suspend fun uploadOrUpdateSyncPayload(
        accessToken: String,
        folderId: String,
        deviceId: String,
        jsonContent: String
    ): Boolean {
        val existingFile = findSyncFileForDevice(accessToken, folderId, deviceId)
        val targetName = "changes_$deviceId.json"
        return if (existingFile != null) {
            updateSyncPayloadFile(accessToken, existingFile.id, jsonContent)
        } else {
            uploadSyncPayload(accessToken, folderId, targetName, jsonContent)
        }
    }

    /**
     * Lists all JSON delta sync files in the family folder.
     */
    suspend fun listRemoteSyncFiles(
        accessToken: String,
        folderId: String
    ): List<DriveFileItem> = withContext(Dispatchers.IO) {
        val query = "'$folderId' in parents and mimeType = 'application/json' and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,createdTime,modifiedTime)&pageSize=100"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Failed to list remote sync files: HTTP ${response.code} - $responseBody")
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(responseBody)
        fileList.files
    }

    /**
     * Downloads content of a specific sync file by fileId.
     */
    suspend fun downloadSyncPayload(
        accessToken: String,
        fileId: String
    ): String = withContext(Dispatchers.IO) {
        val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"

        val request = Request.Builder()
            .url(downloadUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Failed to download payload: HTTP ${response.code} - $responseBody")
        }

        responseBody
    }
}
