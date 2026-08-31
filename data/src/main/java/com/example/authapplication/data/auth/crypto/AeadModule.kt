package com.example.authapplication.data.auth.crypto

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 認証情報(auth_prefs)を暗号化するための Tink [Aead] を提供するモジュール。
 *
 * keysetのマスターキーは Android Keystore(`android-keystore://`)で保護し、
 * keyset自体はデータ本体(auth_prefs.pb)とは別の SharedPreferences に保存する。
 */
@Module
@InstallIn(SingletonComponent::class)
object AeadModule {

    private const val MASTER_KEY_URI = "android-keystore://auth_prefs_master_key"
    private const val KEYSET_PREF_NAME = "auth_prefs_keyset"
    private const val KEYSET_PREF_KEY = "auth_prefs_keyset_key"

    @Provides
    @Singleton
    fun provideAead(@ApplicationContext context: Context): Aead {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_PREF_KEY, KEYSET_PREF_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(Aead::class.java)
    }
}
