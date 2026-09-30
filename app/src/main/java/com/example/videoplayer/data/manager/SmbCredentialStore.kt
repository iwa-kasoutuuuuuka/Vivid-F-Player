package com.example.videoplayer.data.manager

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import java.security.KeyStore
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SMBの認証情報を Android Keystore (AES-GCM) で暗号化して保存します。
 * URLには認証情報を含めず、「ホーム[:ポート]/共有名」単位で保持し、接続時に jcifs の CIFSContext として渡します。
 *
 * Stores SMB credentials encrypted with an Android Keystore AES-GCM key. URLs never carry
 * credentials; they are kept per "host[:port]/share" and supplied to jcifs as a CIFSContext.
 */
class SmbCredentialStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val contexts = ConcurrentHashMap<String, CIFSContext>()

    init {
        migrateLegacyUrls()
    }

    fun save(url: String, username: String, password: String) {
        val key = shareKey(url) ?: return
        contexts.remove(key)
        if (username.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        prefs.edit().putString(key, encrypt("$username$SEPARATOR$password")).apply()
    }

    /** 同じ共有を使うフォルダが残っていなければ削除 / Removes credentials unless another folder still uses the share */
    fun remove(url: String, stillRegistered: Collection<String>) {
        val key = shareKey(url) ?: return
        if (stillRegistered.any { shareKey(it) == key }) return
        contexts.remove(key)
        prefs.edit().remove(key).apply()
    }

    /** 指定URLに接続するための CIFSContext / The CIFSContext to use for a given smb:// URL */
    fun contextFor(url: String): CIFSContext {
        val base = SingletonContext.getInstance()
        val key = shareKey(url) ?: return base
        return contexts.getOrPut(key) {
            val (username, password) = load(key) ?: return base
            // "DOMAIN;user" または "DOMAIN\user" 形式のドメイン指定に対応 / Accept DOMAIN;user or DOMAIN\user
            val sep = username.indexOfFirst { it == ';' || it == '\\' }
            val domain = if (sep > 0) username.substring(0, sep) else null
            val user = if (sep > 0) username.substring(sep + 1) else username
            base.withCredentials(NtlmPasswordAuthenticator(domain, user, password))
        }
    }

    private fun load(key: String): Pair<String, String>? {
        val stored = prefs.getString(key, null) ?: return null
        val plain = decrypt(stored)
        if (plain == null) {
            // バックアップ復元後など鍵が変わって復号できない場合は破棄（再登録が必要）
            // Undecryptable (e.g. restored on another device with a different key): drop it
            prefs.edit().remove(key).apply()
            return null
        }
        val i = plain.indexOf(SEPARATOR)
        return if (i < 0) plain to "" else plain.substring(0, i) to plain.substring(i + 1)
    }

    // ---- 旧形式からの移行 / Migration from credential-carrying URLs (v1.2.17 and earlier) ----

    private fun migrateLegacyUrls() {
        val appPrefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val folders = appPrefs.getStringSet("folder_uris", emptySet()) ?: emptySet()
        if (folders.none { hasCredentials(it) }) return

        val migrated = folders.map { url ->
            if (!hasCredentials(url)) return@map url
            val (username, password) = parseLegacyUserInfo(url)
            val clean = stripCredentials(url)
            save(clean, username, password)
            clean
        }.toSet()
        appPrefs.edit().putStringSet("folder_uris", migrated).apply()

        // レジューム情報のキー/値に含まれていたパスワードも除去 / Scrub passwords out of resume keys/values too
        val resumePrefs = appContext.getSharedPreferences("resume_prefs", Context.MODE_PRIVATE)
        val editor = resumePrefs.edit()
        for ((k, v) in resumePrefs.all) {
            val newKey = stripCredentialsAnywhere(k)
            val newValue = (v as? String)?.let { stripCredentialsAnywhere(it) } ?: v
            if (newKey == k && newValue == v) continue
            editor.remove(k)
            when (newValue) {
                is String -> editor.putString(newKey, newValue)
                is Long -> editor.putLong(newKey, newValue)
                is Int -> editor.putInt(newKey, newValue)
                is Boolean -> editor.putBoolean(newKey, newValue)
                is Float -> editor.putFloat(newKey, newValue)
            }
        }
        editor.apply()
    }

    private fun parseLegacyUserInfo(url: String): Pair<String, String> {
        val authority = url.removePrefix("smb://").substringBefore('/')
        val userInfo = authority.substringBeforeLast('@')
        val user = userInfo.substringBefore(':')
        val pass = if (userInfo.contains(':')) userInfo.substringAfter(':') else ""
        return Uri.decode(user) to Uri.decode(pass)
    }

    // ---- 暗号化 / Encryption ----

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + body, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String? = try {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes, 0, IV_LENGTH))
        }
        String(cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val PREFS_NAME = "smb_credentials"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "vivid_smb_credentials"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val SEPARATOR = '\u0000'

        @Volatile private var instance: SmbCredentialStore? = null

        fun getInstance(context: Context): SmbCredentialStore =
            instance ?: synchronized(this) {
                instance ?: SmbCredentialStore(context).also { instance = it }
            }

        /** "host[:port]/share" (小文字) / lower-cased "host[:port]/share" for an smb:// URL */
        fun shareKey(url: String): String? {
            if (!url.startsWith("smb://", ignoreCase = true)) return null
            val parts = stripCredentials(url).substring(6).split('/').filter { it.isNotEmpty() }
            if (parts.size < 2) return null
            return "${parts[0]}/${parts[1]}".lowercase()
        }

        fun hasCredentials(url: String): Boolean =
            url.startsWith("smb://", ignoreCase = true) && url.substring(6).substringBefore('/').contains('@')

        /** URL先頭の認証情報を除去 / Removes the user-info part from an smb:// URL */
        fun stripCredentials(url: String): String {
            if (!hasCredentials(url)) return url
            val rest = url.substring(6)
            val authority = rest.substringBefore('/')
            return "smb://" + authority.substringAfterLast('@') + rest.substring(authority.length)
        }

        private val LEGACY_URL = Regex("smb://[^/\\s]*@")

        private fun stripCredentialsAnywhere(text: String): String = LEGACY_URL.replace(text, "smb://")
    }
}
