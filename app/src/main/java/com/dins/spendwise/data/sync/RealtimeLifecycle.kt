package com.dins.spendwise.data.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeLifecycle @Inject constructor(
    private val realtimeSubscriber: RealtimeSubscriber,
    private val syncScheduler: SyncScheduler
) : DefaultLifecycleObserver {

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        syncScheduler.enqueueSync()
        realtimeSubscriber.start()
    }

    override fun onStop(owner: LifecycleOwner) {
        realtimeSubscriber.stop()
    }
}
