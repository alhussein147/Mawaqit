package com.hussein.mawaqit.data.azkar


import android.content.Context
import com.hussein.mawaqit.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AzkarCategory(
    val title  : String,
    val content: List<Zikr>
)

@Serializable
data class Zikr(
    val zekr  : String,
    val repeat: Int,
    val bless : String
)


class AzkarRepository(private val context: Context) {

    private val files = listOf(
        "azkar_morning.json",
        "azkar_evening.json",
        "azkar_after_prayer.json"
    )

    // Category titles for the picker screen — no file I/O needed
    val categoryTitles = context.resources.getStringArray(R.array.azkar_titles).toList()


    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadCategory(index: Int): AzkarCategory = withContext(Dispatchers.IO) {
        context.assets.open(files[index]).bufferedReader().use { reader ->
            json.decodeFromString(reader.readText())
        }
    }
}