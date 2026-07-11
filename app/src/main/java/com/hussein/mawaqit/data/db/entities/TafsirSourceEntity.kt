package com.hussein.mawaqit.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "tafsir_sources"
)
data class TafsirSourceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val nameAr: String,
    val lang: String,
    val url: String,
    val downloaded: Boolean = false,
    val isActive: Boolean = false
) {
    companion object {
        val MUKHTASAR = TafsirSourceEntity(
            id = "mukhtasar",
            name = "Tafsir Al-Mukhtasar",
            nameAr = "تفسير المختصر",
            lang = "ar",
            url = "https://dvtajbmeveppcffgfnog.supabase.co/storage/v1/object/public/assets/tafsir/mukhtasar.json"
        )
    }
}
