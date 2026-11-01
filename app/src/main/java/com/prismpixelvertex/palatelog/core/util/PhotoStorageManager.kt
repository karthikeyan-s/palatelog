package com.prismpixelvertex.palatelog.core.util

import android.net.Uri
import java.io.File

/**
 * Domain abstraction for managing temporary photo capture URIs and persisting
 * captured or picked images into permanent application storage.
 */
interface PhotoStorageManager {
    fun createTempCameraUri(): Pair<File, Uri>
    fun copyUriToInternalStorage(sourceUri: Uri): String
}
