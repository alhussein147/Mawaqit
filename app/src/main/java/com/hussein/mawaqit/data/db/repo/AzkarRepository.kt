package com.hussein.mawaqit.data.db.repo

import com.hussein.mawaqit.data.db.dao.InvocationDao
import com.hussein.mawaqit.data.db.entities.invocation.InvocationCategoryEntity
import com.hussein.mawaqit.data.db.entities.invocation.InvocationEntity
import com.hussein.mawaqit.domain.InvocationCategory
import com.hussein.mawaqit.domain.InvocationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AzkarRepository(
    private val invocationDao: InvocationDao
) {


    fun getAllCategories(): Flow<List<InvocationCategory>> =
        invocationDao.getAllCategories().map { entities ->
            entities.map {
                InvocationCategory(
                    id = it.id,
                    title = it.name,
                    highlight = it.highlight,
                    type = it.type.name,
                    sortOrder = it.sortOrder
                )
            }
        }

    fun getInvocationsByCategory(categoryId: Int): Flow<List<InvocationItem>> =
        invocationDao.getInvocationsByCategory(categoryId = categoryId).map { invocationEntities ->
            invocationEntities.map {
                InvocationItem(
                    text = it.content,
                    repeat = it.repeatCount,
                    bless = it.benefit
                )
            }
        }


    suspend fun countCategories(): Int = invocationDao.countCategories()

    suspend fun saveInvocations(
        categories: List<InvocationCategoryEntity>,
        items: List<InvocationEntity>
    ) {
        withContext(Dispatchers.IO) {
            invocationDao.insertCategories(categories)
            invocationDao.insertInvocations(items)
        }
    }
}