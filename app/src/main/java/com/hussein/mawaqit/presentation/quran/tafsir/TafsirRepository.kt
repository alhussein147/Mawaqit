package com.hussein.mawaqit.presentation.quran.tafsir

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class TafsirResponse(
    val code: Int,
    val status: String,
    val data: TafsirData
)

@Serializable
private data class TafsirData(
    val number: Int,
    val text: String,
    val numberInSurah: Int
)

class TafsirRepository {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // In-memory cache — keyed by "surahIndex:ayahNumber"
    private val cache = mutableMapOf<String, String>()

    suspend fun fetchTafsir(surahIndex: Int, ayahNumber: Int): String {
        val key = "$surahIndex:$ayahNumber"
        cache[key]?.let { return it }

        val url = "https://api.qurani.ai/gw/qh/v1/ayah/$surahIndex:$ayahNumber/ar.mukhtasar"
        val response = client.get(url).body<TafsirResponse>()
        val text = response.data.text

        cache[key] = text
        return text
    }

    fun close() = client.close()
}
