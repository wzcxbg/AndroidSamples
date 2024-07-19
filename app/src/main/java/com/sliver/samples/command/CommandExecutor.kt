package com.sliver.samples.command

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

object CommandExecutor {
    private val executor = Executors.newSingleThreadExecutor()
    private val threadPool = Executors.newFixedThreadPool(2)

    fun submit(
        command: String,
        envp: Array<String> = emptyArray(),
        dir: File? = null
    ): Future<CommandOutputs> {
        return executor.submit(Callable {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec(command, emptyArray(), null)
            val readInputTask = threadPool.submit(Callable {
                val totalBuffer = ByteArrayOutputStream()
                val input = process.inputStream
                val buffer = ByteArray(8196)
                var len: Int
                while (input.read(buffer).also { len = it } > 0) {
                    totalBuffer.write(buffer, 0, len)
                }
                totalBuffer.toByteArray()
            })
            val readErrorTask = threadPool.submit(Callable {
                val totalBuffer = ByteArrayOutputStream()
                val error = process.errorStream
                val buffer = ByteArray(8196)
                var len: Int
                while (error.read(buffer).also { len = it } > 0) {
                    totalBuffer.write(buffer, 0, len)
                }
                totalBuffer.toByteArray()
            })
            process.waitFor()
            val inputBytes = readInputTask.get()
            val errorBytes = readErrorTask.get()
            val exitValue = process.exitValue()
            CommandOutputs(inputBytes, errorBytes, exitValue)
        })
    }
}