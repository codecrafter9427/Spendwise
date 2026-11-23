package com.dins.spendwise.data.sync

import com.dins.spendwise.BuildConfig
import com.dins.spendwise.data.remote.AuthRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Named
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Foreground-only SSE listener. Wakes [SyncScheduler] on sync events.
 * Missed events are repaired by pull-on-resume / WorkManager.
 */
@Singleton
class RealtimeSubscriber @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    @Named("realtime") private val okHttpClient: OkHttpClient,
    private val applicationScope: CoroutineScope
) {
    private val running = AtomicBoolean(false)
    private var job: Job? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        job = applicationScope.launch {
            var attempt = 0
            while (isActive && running.get()) {
                val token = authRepository.validAccessToken()
                if (token == null) {
                    delay(5_000)
                    continue
                }
                try {
                    listen(token)
                    attempt = 0
                } catch (_: Exception) {
                    attempt += 1
                    val backoff = minOf(30_000L, 1_000L * (1L shl minOf(attempt, 4)))
                    delay(backoff)
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        job?.cancel()
        job = null
    }

    private fun listen(token: String) {
        val url = BuildConfig.API_BASE_URL.trimEnd('/') + "/api/v1/realtime/events"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "text/event-stream")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Realtime HTTP ${response.code}")
            }
            val body = response.body ?: error("Empty realtime body")
            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                var eventName: String? = null
                while (running.get()) {
                    val line = reader.readLine() ?: break
                    when {
                        line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                        line.startsWith("data:") -> {
                            if (eventName == "sync") {
                                syncScheduler.enqueueSync()
                            }
                            eventName = null
                        }
                        line.isBlank() -> eventName = null
                    }
                }
            }
        }
    }
}
