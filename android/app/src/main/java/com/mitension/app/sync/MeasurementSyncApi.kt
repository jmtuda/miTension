package com.mitension.app.sync

import com.mitension.app.data.LocalMeasurement
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

data class RemoteMeasurement(
    val id: String,
    val measuredAt: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val notes: String?,
    val deletedAt: String?,
    val updatedAt: String,
)

data class SyncPosition(val updatedAt: String, val id: String)

interface MeasurementSyncApi {
    suspend fun create(session: SupabaseSession, local: LocalMeasurement): RemoteMeasurement
    suspend fun delete(session: SupabaseSession, local: LocalMeasurement): RemoteMeasurement
    suspend fun changes(session: SupabaseSession, after: SyncPosition?, limit: Int): List<RemoteMeasurement>
}

class SupabaseRestMeasurementSyncApi : MeasurementSyncApi {
    override suspend fun create(session: SupabaseSession, local: LocalMeasurement): RemoteMeasurement {
        val body = JSONObject()
            .put("id", local.id)
            .put("measured_at", Instant.ofEpochMilli(local.measuredAt).toString())
            .put("systolic", local.systolic)
            .put("diastolic", local.diastolic)
            .put("pulse", local.pulse)
            .put("notes", local.notes ?: JSONObject.NULL)
        val created = request(session, "POST", "/rest/v1/measurements?on_conflict=id&select=$SELECT", body.toString(), "resolution=ignore-duplicates,return=representation")
        return parseRows(created).firstOrNull() ?: fetchById(session, local.id).also { remote ->
            check(Instant.parse(remote.measuredAt) == Instant.ofEpochMilli(local.measuredAt) &&
                remote.systolic == local.systolic && remote.diastolic == local.diastolic &&
                remote.pulse == local.pulse && remote.notes == local.notes) {
                "server row with this id does not match the local measurement"
            }
        }
    }

    override suspend fun delete(session: SupabaseSession, local: LocalMeasurement): RemoteMeasurement {
        val deletedAt = checkNotNull(local.deletedAt) { "pending delete requires deletedAt" }
        val path = "/rest/v1/measurements?id=eq.${encode(local.id)}&deleted_at=is.null&select=$SELECT"
        val response = request(session, "PATCH", path, JSONObject().put("deleted_at", Instant.ofEpochMilli(deletedAt).toString()).toString(), "return=representation")
        return parseRows(response).firstOrNull() ?: fetchById(session, local.id).also {
            check(it.deletedAt != null) { "server did not retain the deletion tombstone" }
        }
    }

    override suspend fun changes(session: SupabaseSession, after: SyncPosition?, limit: Int): List<RemoteMeasurement> {
        val filter = after?.let {
            "&or=(updated_at.gt.${encode(it.updatedAt)},and(updated_at.eq.${encode(it.updatedAt)},id.gt.${encode(it.id)}))"
        }.orEmpty()
        val path = "/rest/v1/measurements?select=$SELECT$filter&order=updated_at.asc,id.asc&limit=$limit"
        return parseRows(request(session, "GET", path))
    }

    private fun fetchById(session: SupabaseSession, id: String): RemoteMeasurement {
        val rows = parseRows(request(session, "GET", "/rest/v1/measurements?id=eq.${encode(id)}&select=$SELECT&limit=1"))
        return rows.singleOrNull() ?: throw IOException("measurement was not visible after an idempotent operation")
    }

    private fun request(session: SupabaseSession, method: String, path: String, body: String? = null, prefer: String? = null): String {
        check(session.publishableKey != session.accessToken) { "an authenticated user session is required" }
        val connection = URI(session.baseUrl + path).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("apikey", session.publishableKey)
            connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            connection.setRequestProperty("Content-Type", "application/json")
            prefer?.let { connection.setRequestProperty("Prefer", it) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("Supabase request failed with HTTP $status")
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRows(value: String): List<RemoteMeasurement> {
        val array = JSONArray(value.ifBlank { "[]" })
        return (0 until array.length()).map { index ->
            val row = array.getJSONObject(index)
            RemoteMeasurement(
                id = row.getString("id"), measuredAt = row.getString("measured_at"),
                systolic = row.getInt("systolic"), diastolic = row.getInt("diastolic"), pulse = row.getInt("pulse"),
                notes = row.optString("notes").takeUnless { row.isNull("notes") },
                deletedAt = row.optString("deleted_at").takeUnless { row.isNull("deleted_at") },
                updatedAt = row.getString("updated_at"),
            )
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    companion object {
        private const val SELECT = "id,measured_at,systolic,diastolic,pulse,notes,deleted_at,updated_at"
    }
}
