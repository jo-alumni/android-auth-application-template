package com.example.authapplication.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.example.authapplication.data.auth.AuthPrefsSerializer
import com.example.authapplication.data.auth.proto.AuthPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideAuthPrefsDataStore(
        @ApplicationContext context: Context,
        serializer: AuthPrefsSerializer,
    ): DataStore<AuthPrefs> = DataStoreFactory.create(
        serializer = serializer,
        corruptionHandler = ReplaceFileCorruptionHandler { AuthPrefs.getDefaultInstance() },
        produceFile = { File(context.filesDir, "datastore/auth_prefs.pb") },
    )
}
