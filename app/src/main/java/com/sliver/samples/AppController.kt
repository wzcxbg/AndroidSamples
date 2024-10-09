package com.sliver.samples

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class AppController {
    private var process: Process? = null
    private var isInitialized: Boolean = false
    private var executor: ScheduledExecutorService? = null

    fun initialize(messageListener: MessageListener? = null) {
        if (isInitialized)
            throw RuntimeException("AppController already initialized!")
        isInitialized = true

        process = Runtime.getRuntime().exec("su")
        executor = Executors.newSingleThreadScheduledExecutor()
        val outputReader = process?.inputStream?.bufferedReader(Charsets.UTF_8)
        val errorReader = process?.errorStream?.bufferedReader(Charsets.UTF_8)
        executor?.scheduleWithFixedDelay({
            outputReader?.takeIf { it.ready() }
                ?.runCatching { outputReader.readLine() }
                ?.onSuccess { messageListener?.onOutput(it) }
            errorReader?.takeIf { it.ready() }
                ?.runCatching { errorReader.readLine() }
                ?.onSuccess { messageListener?.onError(it) }
        }, 0, 1, TimeUnit.MILLISECONDS)
    }

    fun execute(command: String) {
        if (!isInitialized)
            throw RuntimeException("AppController not initialized!")
        process?.outputStream?.write(command.toByteArray(Charsets.UTF_8))
        process?.outputStream?.write('\n'.code)
        process?.outputStream?.flush()
    }

    fun shutdown() {
        executor?.shutdown()
        executor?.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)
        executor = null
        process?.destroy()
        process?.waitFor()
        process = null
        isInitialized = false
    }

    interface MessageListener {
        fun onOutput(outputMsg: String)
        fun onError(errorMsg: String)
    }
}