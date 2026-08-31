package com.mitension.app.sync

data class SupabaseSession(
    val baseUrl: String,
    val publishableKey: String,
    val accessToken: String,
    val userId: String,
)
