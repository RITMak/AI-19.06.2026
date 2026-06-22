package com.aichat.core.di

import com.aichat.core.data.datastore.SettingsDataStore
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    // SettingsDataStore сам @Singleton @Inject constructor
}