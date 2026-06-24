package com.hussein.mawaqit.data.db.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.entities.SurahEntity

data class SurahWithAyahs(
    @Embedded val surah: SurahEntity,
    @Relation(
        parentColumn = "number",
        entityColumn = "surahNumber"
    )
    val ayahs: List<AyahEntity>
)