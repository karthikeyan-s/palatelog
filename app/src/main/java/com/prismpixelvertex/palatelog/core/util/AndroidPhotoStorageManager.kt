package com.prismpixelvertex.palatelog.core.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPhotoStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoStorageManager {

    override fun createTempCameraUri(): Pair<File, Uri> {
        return MediaUtils.createTempCameraUri(context)
    }

    override fun copyUriToInternalStorage(sourceUri: Uri): String {
        return MediaUtils.copyUriToInternalStorage(context, sourceUri)
    }
}
