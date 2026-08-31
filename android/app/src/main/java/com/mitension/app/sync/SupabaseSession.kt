package com.mitension.app.sync

data class SupabaseSession(
    val baseUrl: String,
    val anonKey: String,
    val accessToken: String,
    val userId: String,
)
