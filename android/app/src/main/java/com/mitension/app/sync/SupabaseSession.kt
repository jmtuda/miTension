package com.mitension.app.sync

import android.content.Context

data class SupabaseSession(
    val baseUrl: String,
    val anonKey: String,
    val accessToken: String,
    val userId: String,
)

fun interface SessionProvider {
    fun currentSession(): SupabaseSession?
}

/**
 * Adapter for the persisted Supabase Auth session. The public anon key and the user's short-lived
 * access token are accepted; an administrative service_role key must never be stored here.
 */
class SharedPreferencesSessionProvider(context: Context) : SessionProvider {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun currentSession(): SupabaseSession? {
        val baseUrl = preferences.getString(KEY_URL, null)?.trimEnd('/') ?: return null
        val anonKey = preferences.getString(KEY_ANON_KEY, null) ?: return null
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        return SupabaseSession(baseUrl, anonKey, accessToken, userId)
    }

    companion object {
        const val FILE_NAME = "supabase_auth_session"
        const val KEY_URL = "supabase_url"
        const val KEY_ANON_KEY = "supabase_anon_key"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_USER_ID = "user_id"
    }
}
