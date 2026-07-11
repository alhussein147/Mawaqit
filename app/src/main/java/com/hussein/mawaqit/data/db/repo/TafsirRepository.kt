package com.hussein.mawaqit.data.db.repo

import androidx.room.withTransaction
import com.hussein.mawaqit.data.db.AppDatabase
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity
import com.hussein.mawaqit.data.mappers.toAyah
import com.hussein.mawaqit.data.mappers.toTafsir
import com.hussein.mawaqit.data.remote.RemoteService
import com.hussein.mawaqit.data.remote.dto.TafsirSourceDto
import com.hussein.mawaqit.domain.models.AyahWithTafsir
import com.hussein.mawaqit.domain.models.Tafsir
import com.hussein.mawaqit.infrastructure.settings.QuranReaderPreferences
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow

class TafsirRepository(
    val db: AppDatabase,
    private val quranReaderPreferences: QuranReaderPreferences
) {

    private val tafsirDao = db.tafsirDao()
    private val ayahDao = db.ayahDao()

    fun getAvailableTafsirSources(): Flow<List<TafsirSourceEntity>> = tafsirDao.getAllSources()

    suspend fun syncTafsirSources() {
        try {
            val dtos: List<TafsirSourceDto> = RemoteService.getClient().get(TAFSIR_SOURCES_URL).body()
            
            db.withTransaction {
                dtos.forEach { dto ->
                    val existing = tafsirDao.getSourceById(dto.id)
                    if (existing == null) {
                        tafsirDao.upsertSource(TafsirSourceEntity(
                            id = dto.id,
                            name = dto.name,
                            nameAr = dto.nameAr,
                            lang = dto.language,
                            url = dto.url,
                            downloaded = false,
                            isActive = dto.id == "mukhtasar" // Default active
                        ))
                    } else {
                        tafsirDao.updateSourceMetadata(
                            id = dto.id,
                            name = dto.name,
                            nameAr = dto.nameAr,
                            lang = dto.language,
                            url = dto.url
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (tafsirDao.getAllSourcesList().isEmpty()) {
                tafsirDao.upsertSource(TafsirSourceEntity.MUKHTASAR.copy(isActive = true))
            }
        }
    }

    val selectedTafsirSourceId: Flow<String?> = tafsirDao.getActiveSourceId()

    suspend fun setSelectedTafsirSourceId(id: String) {
        tafsirDao.setActiveSource(id)
    }

    suspend fun fetchTafsirForAyah(
        sourceId: String,
        surahNumber: Int,
        ayahNumber: Int
    ): Tafsir? {
        return tafsirDao.getTafsirForAyah(
            sourceId = sourceId,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber
        )?.toTafsir()
    }

    suspend fun fetchSurahWithTafsir(
        sourceId: String,
        surahNumber: Int
    ): List<AyahWithTafsir> {
        val ayahs = ayahDao.getAyahsForSurah(surahNumber).map { it.toAyah() }
        val tafsirs = tafsirDao.getTafsirForSurah(sourceId, surahNumber).associateBy { it.numberInSurah }

        return ayahs.map { ayah ->
            AyahWithTafsir(
                ayah = ayah,
                tafsir = tafsirs[ayah.numberInSurah]?.toTafsir()
            )
        }
    }

    companion object {
        private const val TAFSIR_SOURCES_URL = "https://dvtajbmeveppcffgfnog.supabase.co/storage/v1/object/public/assets/tafsir/sources.json"
    }
}
