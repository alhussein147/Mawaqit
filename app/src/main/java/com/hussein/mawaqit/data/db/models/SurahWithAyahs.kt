package com.hussein.mawaqit.data.db.models

import androidx.room.Embedded
import androidx.room.Relation
import com.hussein.mawaqit.data.db.AyahEntity
import com.hussein.mawaqit.data.db.SurahEntity

data class SurahWithAyahs(
    @Embedded val surah: SurahEntity,
    @Relation(
        parentColumn = "number",
        entityColumn = "surahNumber"
    )
    val ayahs: List<AyahEntity>
)