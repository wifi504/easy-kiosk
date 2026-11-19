package top.lhlnb.easykiosk

import android.os.SystemClock
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

internal data class NetworkTimeSample(val unixMillis: Long, val receivedAt: Long)

internal object NetworkTimeConsensus {
    fun resolve(samples: List<NetworkTimeSample>, now: Long): Long {
        val times = samples.map { it.unixMillis + (now - it.receivedAt) }.sorted()
        require(times.size >= 2) { "Network time unavailable" }
        for (index in 0 until times.size - 1) {
            if (times[index + 1] - times[index] <= 5_000) return times[index] + (times[index + 1] - times[index]) / 2
        }
        error("Network time sources disagree")
    }
}

/** Two independently hosted HTTPS Date headers must agree. Never falls back to local time. */
internal object NetworkTime {
    private val endpoints = listOf("https://www.aliyun.com/", "https://cloud.tencent.com/", "https://www.baidu.com/")

    @Volatile private var prepared: NetworkTimeSample? = null

    fun prepare() {
        prepared = null
        val unix = fetchUnixMillis()
        prepared = NetworkTimeSample(unix, SystemClock.elapsedRealtime())
    }

    fun preparedUnixMillis(): Long {
        val sample = prepared ?: error("Network time not ready")
        val now = SystemClock.elapsedRealtime()
        check(now - sample.receivedAt in 0..90_000) { "Network time expired" }
        return sample.unixMillis + now - sample.receivedAt
    }

    fun fetchUnixMillis(): Long {
        val pool = Executors.newFixedThreadPool(endpoints.size)
        try {
            val tasks = endpoints.map { endpoint -> Callable { sample(endpoint) } }
            val samples = pool.invokeAll(tasks, 7, TimeUnit.SECONDS).mapNotNull { future ->
                try { if (future.isCancelled) null else future.get() } catch (_: Exception) { null }
            }
            return NetworkTimeConsensus.resolve(samples, SystemClock.elapsedRealtime())
        } finally { pool.shutdownNow() }
    }

    private fun sample(endpoint: String): NetworkTimeSample? {
        val connection = URL(endpoint + "?easykiosk_time=" + java.util.UUID.randomUUID()).openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "HEAD"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
            val start = SystemClock.elapsedRealtime()
            if (connection.responseCode !in 200..399) return null
            val received = SystemClock.elapsedRealtime()
            if (received - start > 5_000) return null
            val age = connection.getHeaderField("Age")?.toLongOrNull() ?: 0
            if (age !in 0..5) return null
            val header = connection.getHeaderField("Date") ?: return null
            val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT"); isLenient = false
            }
            val unix = parser.parse(header)?.time ?: return null
            if (unix < 1_577_836_800_000L) return null
            return NetworkTimeSample(unix + age * 1000 + (received - start) / 2, received)
        } finally { connection.disconnect() }
    }
}
