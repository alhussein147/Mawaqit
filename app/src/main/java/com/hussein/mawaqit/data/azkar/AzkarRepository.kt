package com.hussein.mawaqit.data.azkar


import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AzkarCategory(
    val id: Int,
    val category: String,
    @SerialName("array")
    val content: List<Zikr>
)

@Serializable
data class Zikr(
    val id: Int,
    val text: String,
    val count: Int
)

@Serializable
data class AzkarMeta(
    val id: Int,
    val file: String,
    val title: String
)


class AzkarRepository(private val context: Context) {

    private val files = listOf(
        "azkar_morning.json",
        "azkar_evening.json",
        "azkar_after_prayer.json"
    )

    private val json = Json { ignoreUnknownKeys = true }

    private var cachedMeta: List<AzkarMeta>? = null

    suspend fun loadMetadata(): List<AzkarMeta> = withContext(Dispatchers.IO) {
        cachedMeta ?: context.assets.open("azkar/metadata.json")
            .bufferedReader()
            .use { json.decodeFromString<List<AzkarMeta>>(it.readText()) }
            .also { cachedMeta = it }
    }

    suspend fun loadCategory(index: Int): AzkarCategory = withContext(Dispatchers.IO) {
        val meta = loadMetadata()
        val fileName = meta.getOrNull(index)?.file
            ?: "${(index + 1).toString().padStart(3, '0')}.json"
        context.assets.open("azkar/$fileName").bufferedReader().use { reader ->
            json.decodeFromString(reader.readText())
        }
    }
}