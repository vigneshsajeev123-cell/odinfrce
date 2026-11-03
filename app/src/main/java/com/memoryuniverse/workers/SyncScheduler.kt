package com.memoryuniverse.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Battery-conscious WorkManager scheduler enforcing zero-network offline constraints.
 */
object SyncScheduler {

    /**
     * Schedules periodic background sync (every 6 hours) with battery-not-low constraints.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val periodicMediaWork = PeriodicWorkRequestBuilder<MediaImportWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MediaImportWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicMediaWork
        )
    }

    /**
     * Triggers immediate one-time sync and clustering in background.
     */
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val importWork = OneTimeWorkRequestBuilder<MediaImportWorker>()
            .setConstraints(constraints)
            .build()

        val clusteringWork = OneTimeWorkRequestBuilder<ClusteringWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork("ImmediateSyncAndCluster", ExistingWorkPolicy.REPLACE, importWork)
            .then(clusteringWork)
            .enqueue()
    }
}
