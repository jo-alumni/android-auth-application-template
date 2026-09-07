package com.example.authapplication.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
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

    /**
     * お気に入りIDの保存先。秘匿情報ではないので暗号化せず、
     * スキーマ定義の不要なPreferences DataStoreを使う。
     */
    @Provides
    @Singleton
    fun provideFavoritePrefsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { File(context.filesDir, "datastore/favorite_prefs.preferences_pb") },
    )
}
