package com.corps.healthmate.workers



import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.corps.healthmate.repository.GamificationRepository

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DailyResetWorker(
    @ApplicationContext context: Context,
    params: WorkerParameters,
    private val repository: GamificationRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        repository.resetDailySteps()
        return Result.success()
    }
}