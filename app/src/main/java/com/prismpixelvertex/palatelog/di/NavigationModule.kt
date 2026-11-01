package com.prismpixelvertex.palatelog.di

import com.prismpixelvertex.palatelog.core.navigation.AndroidMapNavigator
import com.prismpixelvertex.palatelog.core.navigation.MapNavigator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {

    @Binds
    @Singleton
    abstract fun bindMapNavigator(
        impl: AndroidMapNavigator
    ): MapNavigator
}
