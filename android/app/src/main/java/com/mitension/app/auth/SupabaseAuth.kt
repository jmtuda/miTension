package com.mitension.app.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mitension.app.sync.SupabaseSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.time.Clock

data class PersistedAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val expiresAtEpochSeconds: Long,
)

interface AuthSessionStore {
    fun read(): PersistedAuthSession?
    fun write(session: PersistedAuthSession)
    fun clear()
}

class SharedPreferencesAuthSessionStore(context: Context) : AuthSessionStore {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun read(): PersistedAuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L).takeIf { it > 0 } ?: return null
        return PersistedAuthSession(accessToken, refreshToken, userId, expiresAt)
    }

    override fun write(session: PersistedAuthSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .apply()
    }

    override fun clear() = preferences.edit().clear().apply()

    companion object {
        const val FILE_NAME = "supabase_auth_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}

interface SupabaseAuthApi {
    suspend fun signIn(email: String, password: String): PersistedAuthSession
    suspend fun refresh(refreshToken: String): PersistedAuthSession
}

class SupabaseRestAuthApi(
    private val baseUrl: String,
    private val anonKey: String,
    private val clock: Clock = Clock.systemUTC(),
) : SupabaseAuthApi {
    override suspend fun signIn(email: String, password: String): PersistedAuthSession = request(
        "password",
        JSONObject().put("email", email).put("password", password),
    )

    override suspend fun refresh(refreshToken: String): PersistedAuthSession = request(
        "refresh_token",
        JSONObject().put("refresh_token", refreshToken),
    )

    private suspend fun request(grantType: String, body: JSONObject): PersistedAuthSession = withContext(Dispatchers.IO) {
        check(baseUrl.isNotBlank() && anonKey.isNotBlank()) { "Supabase no está configurado" }
        val connection = URI("${baseUrl.trimEnd('/')}/auth/v1/token?grant_type=$grantType").toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("apikey", anonKey)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("No se pudo autenticar con Supabase (HTTP $status)")
            parseSession(JSONObject(response), clock)
        } finally {
            connection.disconnect()
        }
    }
}

class SupabaseAuthManager(
    private val baseUrl: String,
    private val anonKey: String,
    private val store: AuthSessionStore,
    private val api: SupabaseAuthApi,
    private val clock: Clock = Clock.systemUTC(),
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && anonKey.isNotBlank()

    suspend fun signIn(email: String, password: String): SupabaseSession {
        val persisted = api.signIn(email.trim(), password)
        store.write(persisted)
        return persisted.toSession()
    }

    suspend fun validSession(): SupabaseSession? {
        val stored = store.read() ?: return null
        if (stored.expiresAtEpochSeconds > clock.instant().epochSecond + REFRESH_MARGIN_SECONDS) return stored.toSession()
        return api.refresh(stored.refreshToken).also(store::write).toSession()
    }

    fun restoredSession(): SupabaseSession? = store.read()?.toSession()

    fun signOut() = store.clear()

    private fun PersistedAuthSession.toSession() = SupabaseSession(baseUrl.trimEnd('/'), anonKey, accessToken, userId)

    companion object { private const val REFRESH_MARGIN_SECONDS = 60L }
}

internal fun parseSession(json: JSONObject, clock: Clock): PersistedAuthSession {
    val expiresIn = json.optLong("expires_in", 0L)
    require(expiresIn > 0) { "Supabase no devolvió una caducidad válida" }
    return PersistedAuthSession(
        accessToken = json.getString("access_token"),
        refreshToken = json.getString("refresh_token"),
        userId = json.getJSONObject("user").getString("id"),
        expiresAtEpochSeconds = clock.instant().epochSecond + expiresIn,
    )
}
