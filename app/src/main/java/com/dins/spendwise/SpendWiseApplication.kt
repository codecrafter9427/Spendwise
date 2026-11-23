package com.dins.spendwise

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dins.spendwise.data.remote.AuthRepository
import com.dins.spendwise.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltAndroidApp
class SpendWiseApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            authRepository.sessionFlow.collectLatest { session ->
                if (session != null) {
                    syncScheduler.enqueueSync()
                }
            }
        }
    }
}
