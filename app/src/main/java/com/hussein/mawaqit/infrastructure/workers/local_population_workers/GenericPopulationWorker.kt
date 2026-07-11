package com.hussein.mawaqit.infrastructure.workers.local_population_workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hussein.mawaqit.R
import com.hussein.mawaqit.infrastructure.notification.NotificationUtils
import com.hussein.mawaqit.infrastructure.workers.local_population_workers.strategies.TafsirPopulationStrategy
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class GenericPopulationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val strategies: List<DataPopulationStrategy> by inject()

    companion object {
        const val KEY_STRATEGY_NAME = "strategy_name"
        const val KEY_SOURCE_ID = "source_id"
        const val KEY_PROGRESS = "progress"
        private const val TAG = "GenericPopulationWorker"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val strategyName = inputData.getString(KEY_STRATEGY_NAME) ?: "Data"
        return createForegroundInfo(strategyName)
    }

    private fun createForegroundInfo(strategyName: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationUtils.SYNC_CHANNEL_ID
        )
            .setContentTitle("Syncing $strategyName")
            .setTicker("Syncing $strategyName")
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    override suspend fun doWork(): Result {
        val strategyName = inputData.getString(KEY_STRATEGY_NAME)
        val sourceId = inputData.getString(KEY_SOURCE_ID)

        var strategy = strategies.find { it.name == strategyName }

        if (strategy == null && strategyName == "Tafsir" && sourceId != null) {
            // Try to load specific tafsir source
            val db: com.hussein.mawaqit.data.db.AppDatabase by inject()
            val source = db.tafsirDao().getSourceById(sourceId)
            if (source != null) {
                strategy = get<TafsirPopulationStrategy> {
                    parametersOf(source)
                }
            }
        }

        if (strategy == null) {
            Log.e(TAG, "No strategy found for name: $strategyName")
            return Result.failure(workDataOf("error" to "Unknown strategy: $strategyName"))
        }

        return try {
            if (strategy.shouldPopulate()) {
                Log.d(TAG, "Starting population for: ${strategy.name}")
                setForeground(createForegroundInfo(strategy.name))
                strategy.execute { progress ->
                    setProgress(workDataOf(KEY_PROGRESS to progress))
                }
                Log.d(TAG, "Population complete for: ${strategy.name}")
                NotificationUtils.showWorkerCompletionNotification(
                    applicationContext,
                    "Data Sync",
                    "${strategy.name} sync completed successfully",
                    true
                )
            } else {
                Log.d(TAG, "Database already populated for ${strategy.name} — skipping")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Population failed for ${strategy.name}", e)
            NotificationUtils.showWorkerCompletionNotification(
                applicationContext,
                "Data Sync",
                "Failed to sync ${strategyName ?: "Data"}: ${e.message}",
                false
            )
            Result.retry()
        }
    }
}
