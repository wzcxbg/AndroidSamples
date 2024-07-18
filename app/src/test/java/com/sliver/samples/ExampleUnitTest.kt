package com.sliver.samples

import android.util.Log
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTerminal() {
        val process = Runtime.getRuntime()
            .exec("cmd")
        val readInputThread = Thread {
            val reader = process.inputStream.bufferedReader(charset("GBK"))
            while (true) {
                Thread.sleep(10)
                val readLine = reader.readLine()
                println("testTerminal: input:$readLine")
            }
        }
        val readErrorThread = Thread {
            val reader = process.errorStream.bufferedReader(charset("GBK"))
            while (true) {
                Thread.sleep(10)
                val readLine = reader.readLine()
                println("testTerminal: error:$readLine")
            }
        }
        readInputThread.start()
        readErrorThread.start()

        val writer = process.outputStream.bufferedWriter()
        writer.write("powershell")
        writer.newLine()
        writer.flush()
        Thread.sleep(3000)

        writer.write("dir")
        writer.newLine()
        writer.flush()
        Thread.sleep(3000)

        writer.write("echo jinitaimei")
        writer.newLine()
        writer.flush()
        Thread.sleep(3000)

        writer.write("exit")
        writer.newLine()
        writer.flush()
        Thread.sleep(3000)
    }
}