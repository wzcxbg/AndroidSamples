package com.sliver.samples.command

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Arrays
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class CommandProcess(
    private val command: String,
    private val envp: Array<String> = emptyArray(),
    private val dir: File? = null
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val threadPool = Executors.newFixedThreadPool(2)
    private val process = Runtime.getRuntime().exec("su")

    fun submit(command: String): Future<CommandOutputs> {
        return executor.submit(Callable {
            val magicDeliver = byteArrayOf(
                'A'.code.toByte(),
                'B'.code.toByte(),
                'C'.code.toByte(),
                'D'.code.toByte(),
                '\n'.code.toByte(),
            )
            val readInputTask = threadPool.submit(Callable {
                val totalBuffer = ByteArrayOutputStream()
                val input = process.inputStream
                val buffer = ByteArray(8196)
                while (true) {
                    val len = if (input.available() > 0)
                        input.read(buffer) else -1
                    if (len > 0) {
                        totalBuffer.write(buffer, 0, len)
                    } else if (totalBuffer.toByteArray().size >= magicDeliver.size &&
                        totalBuffer.toByteArray().let { totalBytes ->
                            Arrays.copyOfRange(
                                totalBytes,
                                totalBytes.size - magicDeliver.size,
                                totalBytes.size
                            )
                        }.contentEquals(magicDeliver)
                    ) {
                        break
                    }
                }
                Arrays.copyOf(totalBuffer.toByteArray(), totalBuffer.toByteArray().size - 5)
            })
            val readErrorTask = threadPool.submit(Callable {
                val totalBuffer = ByteArrayOutputStream()
                val error = process.errorStream
                val buffer = ByteArray(8196)
                while (true) {
                    val len = if (error.available() > 0)
                        error.read(buffer) else -1
                    if (len > 0) {
                        totalBuffer.write(buffer, 0, len)
                    } else if (readInputTask.isDone) {
                        break
                    }
                }
                totalBuffer.toByteArray()
            })
            process.outputStream.write(command.toByteArray())
            process.outputStream.write("\n".toByteArray())
            process.outputStream.flush()
            process.outputStream.write("echo ${String(magicDeliver)}".toByteArray())
            process.outputStream.flush()
            val inputBytes = readInputTask.get()
            val errorBytes = readErrorTask.get()
            CommandOutputs(inputBytes, errorBytes, 0)
        })
    }

    fun shutdown() {
        threadPool.shutdown()
        executor.shutdown()
        process.destroy()
    }
}