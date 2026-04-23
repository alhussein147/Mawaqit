package com.hussein.mawaqit.data.azkar


import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.collections.getOrNull

@Serializable
data class AzkarCategory(
    val title: String,
    @SerialName("content")
    val content: List<Zikr>
)

@Serializable
data class Zikr(
    @SerialName("zekr")
    val text: String,
    val repeat: Int,
    val bless: String
)

@Serializable
data class AzkarMeta(
    val id: Int,
    val file: String,
    val title: String
)
class AzkarRepository(private val context: Context) {


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