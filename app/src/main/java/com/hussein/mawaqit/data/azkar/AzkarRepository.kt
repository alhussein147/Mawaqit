package com.hussein.mawaqit.data.azkar


import android.content.Context
import com.hussein.mawaqit.data.db.dao.AzkarDao
import com.hussein.mawaqit.data.db.entities.AzkarCategoryEntity
import com.hussein.mawaqit.data.db.entities.AzkarItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AzkarRepository(
    private val context: Context,
    private val azkarDao: AzkarDao
) {


    fun getAllCategories(): Flow<List<AzkarCategoryEntity>> = azkarDao.getAllCategories()

    fun getItemsForCategory(categoryId: Int): Flow<List<AzkarItemEntity>> =
        azkarDao.getItemsForCategory(categoryId)

    suspend fun countCategories(): Int = azkarDao.countCategories()

    suspend fun saveAzkar(categories: List<AzkarCategoryEntity>, items: List<AzkarItemEntity>) {
        withContext(Dispatchers.IO) {
            azkarDao.insertCategories(categories)
            azkarDao.insertItems(items)
        }
    }
}
