package com.example.authapplication.data.auth

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.example.authapplication.data.auth.proto.AuthPrefs
import com.google.crypto.tink.Aead
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import javax.inject.Inject

/**
 * [AuthPrefs] を Tink [Aead] で暗号化/復号して読み書きする Proto DataStore用Serializer。
 *
 * 復号・パースに失敗した場合(鍵の不整合やファイル破損)は [CorruptionException] を投げ、
 * DataStore側の corruptionHandler によってデフォルト値(未ログイン状態)にフォールバックさせる。
 */
class AuthPrefsSerializer @Inject constructor(
    private val aead: Aead,
) : Serializer<AuthPrefs> {

    override val defaultValue: AuthPrefs = AuthPrefs.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AuthPrefs {
        val encrypted = input.readBytes()
        if (encrypted.isEmpty()) return defaultValue
        return try {
            val decrypted = aead.decrypt(encrypted, ASSOCIATED_DATA)
            AuthPrefs.parseFrom(decrypted)
        } catch (e: GeneralSecurityException) {
            throw CorruptionException("Failed to decrypt auth prefs.", e)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Failed to parse auth prefs.", e)
        }
    }

    override suspend fun writeTo(t: AuthPrefs, output: OutputStream) {
        val encrypted = aead.encrypt(t.toByteArray(), ASSOCIATED_DATA)
        output.write(encrypted)
    }

    private companion object {
        // AEADの関連付けデータ(AAD)。用途を固定するための識別バイト列。
        val ASSOCIATED_DATA: ByteArray = "auth_prefs".toByteArray()
    }
}
