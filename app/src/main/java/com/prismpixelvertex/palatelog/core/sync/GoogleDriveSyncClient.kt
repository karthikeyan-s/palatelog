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

@Serializable
data class DrivePermissionItem(
    val id: String? = null,
    val emailAddress: String? = null,
    val role: String? = null
)

@Serializable
data class DrivePermissionListResponse(
    val permissions: List<DrivePermissionItem> = emptyList()
)

sealed interface FolderAccessResult {
    object Success : FolderAccessResult
    data class AccessDenied(val folderId: String, val statusCode: Int) : FolderAccessResult
    data class Error(val message: String) : FolderAccessResult
}

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
        val scope = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.readonly"
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
        createNewFamilyFolder(accessToken)
    }

    /**
     * Creates a fresh 'PalateLog_Family' folder in Google Drive.
     */
    suspend fun createNewFamilyFolder(accessToken: String): String = withContext(Dispatchers.IO) {
        val folderName = "PalateLog_Family"
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

    /**
     * Locates or creates the 'Photos' subfolder inside 'PalateLog_Family'.
     */
    suspend fun getOrCreatePhotosFolder(
        accessToken: String,
        familyFolderId: String
    ): String = withContext(Dispatchers.IO) {
        val folderName = "Photos"
        val query = "'$familyFolderId' in parents and name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)"

        val request = Request.Builder()
            .url(searchUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Failed to search Photos folder: HTTP ${response.code} - $responseBody")
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(responseBody)
        if (fileList.files.isNotEmpty()) {
            return@withContext fileList.files.first().id
        }

        val createUrl = "https://www.googleapis.com/drive/v3/files"
        val jsonPayload = """{"name":"$folderName","mimeType":"application/vnd.google-apps.folder","parents":["$familyFolderId"]}"""
        val createRequest = Request.Builder()
            .url(createUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        val createResponse = httpClient.newCall(createRequest).execute()
        val createBody = createResponse.body?.string() ?: ""

        if (!createResponse.isSuccessful) {
            throw IOException("Failed to create Photos folder: HTTP ${createResponse.code} - $createBody")
        }

        val createdFolder = json.decodeFromString<DriveFileItem>(createBody)
        createdFolder.id
    }

    /**
     * Lists all photo files in the 'Photos' subfolder.
     */
    suspend fun listRemotePhotoFiles(
        accessToken: String,
        photosFolderId: String
    ): List<DriveFileItem> = withContext(Dispatchers.IO) {
        val query = "'$photosFolderId' in parents and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,createdTime,modifiedTime)&pageSize=1000"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return@withContext emptyList()
        }

        val fileList = json.decodeFromString<DriveFileListResponse>(responseBody)
        fileList.files
    }

    /**
     * Uploads a binary photo file to the 'Photos' subfolder in Google Drive.
     */
    suspend fun uploadPhotoFile(
        accessToken: String,
        photosFolderId: String,
        fileName: String,
        fileBytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        val boundary = "PalateLogPhotoBoundary${System.currentTimeMillis()}"

        val multipartHeader = StringBuilder().apply {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append("""{"name":"$fileName","parents":["$photosFolderId"]}""")
            append("\r\n--$boundary\r\n")
            append("Content-Type: image/jpeg\r\n\r\n")
        }.toString().toByteArray(Charsets.UTF_8)

        val closingBoundary = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val fullBodyBytes = multipartHeader + fileBytes + closingBoundary

        val request = Request.Builder()
            .url(uploadUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "multipart/related; boundary=$boundary")
            .post(fullBodyBytes.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Failed to upload photo $fileName: HTTP ${response.code} - $responseBody")
        }

        true
    }

    /**
     * Downloads a photo file from Google Drive directly into local device file.
     */
    suspend fun downloadPhotoFile(
        accessToken: String,
        fileId: String,
        destFile: java.io.File
    ): Boolean = withContext(Dispatchers.IO) {
        val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"

        val request = Request.Builder()
            .url(downloadUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful && response.body != null) {
            destFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            response.body!!.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } else {
            false
        }
    }

    /**
     * Verifies if the authenticated user has access to the target Google Drive folder.
     */
    suspend fun verifyFolderAccess(
        accessToken: String,
        folderId: String
    ): FolderAccessResult = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files/$folderId?fields=id,name"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            when (response.code) {
                200 -> FolderAccessResult.Success
                404, 403 -> FolderAccessResult.AccessDenied(folderId, response.code)
                else -> FolderAccessResult.Error("HTTP ${response.code}: ${response.body?.string()}")
            }
        } catch (e: Exception) {
            FolderAccessResult.Error(e.localizedMessage ?: "Network request failed")
        }
    }

    /**
     * Grants 'writer' (Editor) permission on the target Google Drive folder to a family member email.
     */
    suspend fun grantFolderPermission(
        accessToken: String,
        folderId: String,
        emailAddress: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = emailAddress.trim()
        android.util.Log.d("PalateLog_Sync", "grantFolderPermission called for email=$cleanEmail, folderId=$folderId")
        val url = "https://www.googleapis.com/drive/v3/files/$folderId/permissions?sendNotificationEmail=false"
        val jsonPayload = """{"role":"writer","type":"user","emailAddress":"$cleanEmail"}"""

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        android.util.Log.d("PalateLog_Sync", "grantFolderPermission response code: ${response.code}, body: $responseBody")

        if (!response.isSuccessful && response.code != 409) { // 409 Conflict = already granted
            android.util.Log.e("PalateLog_Sync", "grantFolderPermission failed: HTTP ${response.code} - $responseBody")
            throw IOException("HTTP ${response.code} - $responseBody")
        }

        true
    }

    /**
     * Lists current permission holders for the target Google Drive folder.
     */
    suspend fun listFolderPermissions(
        accessToken: String,
        folderId: String
    ): List<DrivePermissionItem> = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files/$folderId/permissions?fields=permissions(id,emailAddress,role)"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return@withContext emptyList()
        }

        try {
            val permList = json.decodeFromString<DrivePermissionListResponse>(responseBody)
            permList.permissions
        } catch (e: Exception) {
            emptyList()
        }
    }
}
