package com.prismpixelvertex.palatelog.di

import com.prismpixelvertex.palatelog.core.util.AndroidPhotoStorageManager
import com.prismpixelvertex.palatelog.core.util.PhotoStorageManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindPhotoStorageManager(
        impl: AndroidPhotoStorageManager
    ): PhotoStorageManager
}
