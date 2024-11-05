package com.sliver.samples

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.sliver.samples.base.BaseActivity
import com.sliver.samples.custom.FriendListAdapter
import com.sliver.samples.databinding.ActivityMainBinding
import com.sliver.samples.screencapture.TestScreenCaptureActivity
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IArchiveOpenCallback
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.Executors


class MainActivity : BaseActivity<ActivityMainBinding>() {
    private val list = listOf(
        FriendListAdapter.Friend("赵...", "世上没有绝望的处境，只有对处境绝望的人。"),
        FriendListAdapter.Friend("孙...", "大多数人想要改造这个世界，但却罕有人想改造自己。 "),
        FriendListAdapter.Friend("李...", "积极的人在每一次忧患中都看到一个机会， 而消极的人则在每个机会都看到某种忧患。"),
        FriendListAdapter.Friend("周...", "莫找借口失败，只找理由成功。"),
        FriendListAdapter.Friend("吴...", "世上没有绝望的处境，只有对处境绝望的人。  "),
        FriendListAdapter.Friend("郑...", "当你感到悲哀痛苦时，最好是去学些什么东西。学习会使你永远立于不败之地。"),
        FriendListAdapter.Friend("王...", "世界上那些最容易的事情中，拖延时间最不费力。 "),
        FriendListAdapter.Friend("冯...", "人之所以能，是相信能。"),
        FriendListAdapter.Friend("陈...", "一个有信念者所开发出的力量，大于99个只有兴趣者。 "),
        FriendListAdapter.Friend("卫...", "每一发奋努力的背后，必有加倍的赏赐。"),
        FriendListAdapter.Friend("沈...", "人生伟业的建立 ，不在能知，乃在能行。"),
    )
    private val adapter = FriendListAdapter()

    companion object {
        init {
            System.loadLibrary("samples")
        }
    }
    private external fun screenCapture()

    override fun initView() {
        adapter.setItems(list)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        )

        binding.hello.setOnClickListener {
            val intent = Intent(this, TestScreenCaptureActivity::class.java)
            startActivity(intent)
        }
//        val controller = AppController()
//        controller.initialize(object : AppController.MessageListener {
//            override fun onOutput(outputMsg: String) {
//                Log.e(TAG, "onOutput: $outputMsg")
//            }
//
//            override fun onError(errorMsg: String) {
//                Log.e(TAG, "onError: $errorMsg")
//            }
//        })
        val executor = Executors.newSingleThreadExecutor()
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0
        )
        binding.terminal.setOnClickListener {
            executor.execute {
                screenCapture()
            }

//            testExtractSlowFile(File("/sdcard/Download/testrar.rar"))
//            controller.execute("ifconfig")
//            controller.execute("ffmpeg")
//            Thread.sleep(3000)
//            controller.execute("input tap 540 1000")
//            controller.shutdown()
        }
    }

    private fun testListArchiveFile(file: File) {
        val randomAccessFile = RandomAccessFile(file, "r")
        val inStream = RandomAccessFileInStream(randomAccessFile)
        val inArchive = SevenZip.openInArchive(null, inStream,
            object : IArchiveOpenCallback {
                override fun setTotal(files: Long?, bytes: Long?) {
                    Log.e(TAG, "testArchiveFile:setTotal $files $bytes")
                }

                override fun setCompleted(files: Long?, bytes: Long?) {
                    Log.e(TAG, "testArchiveFile:setCompleted $files $bytes")
                }
            })

        Log.e(TAG, "testArchiveFile:Archive format: ${inArchive.archiveFormat}")
        Log.e(TAG, "testArchiveFile:Items in archive: ${inArchive.numberOfItems}")

        for (i in 0 until inArchive.numberOfItems) {
            val path = inArchive.getStringProperty(i, PropID.PATH)
            val size = inArchive.getStringProperty(i, PropID.SIZE)
            Log.e(TAG, "testArchiveFile: $path $size")
        }

        inArchive.close()
        inStream.close()
    }

    private fun testExtractArchiveFile(file: File) {
        val randomAccessFile = RandomAccessFile(file, "r")
        val inStream = RandomAccessFileInStream(randomAccessFile)
        val inArchive = SevenZip.openInArchive(null,
            inStream, object : IArchiveOpenCallback {
                override fun setTotal(files: Long?, bytes: Long?) {
                    Log.e(TAG, "testArchiveFile:setTotal $files $bytes")
                }

                override fun setCompleted(files: Long?, bytes: Long?) {
                    Log.e(TAG, "testArchiveFile:setCompleted $files $bytes")
                }
            })

        for (i in 0 until inArchive.numberOfItems) {
            val filePath = inArchive.getStringProperty(i, PropID.PATH)
            val fileSize = inArchive.getStringProperty(i, PropID.SIZE)
            Log.e(TAG, "filePath: $filePath fileSize: $fileSize")
        }
        inArchive.extract(null, false, object : IArchiveExtractCallback {
            override fun setTotal(total: Long) {
                Log.e(TAG, "setTotal: $total")
            }

            override fun setCompleted(complete: Long) {
                Log.e(TAG, "setCompleted: $complete")
            }

            override fun getStream(
                index: Int,
                extractAskMode: ExtractAskMode?
            ): ISequentialOutStream {
                return ISequentialOutStream { data ->
                    Log.e(TAG, "write Data: ${data?.size}")
                    data?.size ?: 0
                }
            }

            override fun prepareOperation(extractAskMode: ExtractAskMode?) {
                Log.e(TAG, "prepareOperation: $extractAskMode")
            }

            override fun setOperationResult(extractOperationResult: ExtractOperationResult?) {
                Log.e(TAG, "setOperationResult: $extractOperationResult")
                if (extractOperationResult != ExtractOperationResult.OK) {
                    throw SevenZipException(extractOperationResult.toString())
                }
            }
        })

        inArchive.close()
        inStream.close()
    }


    private fun testExtractSlowFile(file: File) {
        Log.e(TAG, "initView: ${SevenZip.getSevenZipVersion()}")
        Log.e(TAG, "initView: ${SevenZip.getSevenZipJBindingVersion()}")
        Log.e(TAG, "initView: ${SevenZip.isInitializedSuccessfully()}")
        Log.e(TAG, "testExtractSlowFile: ArchiveFileSize: ${file.length()}")
        val randomAccessFile = RandomAccessFile(file, "r")
        val inStream = RandomAccessFileInStream(randomAccessFile)
        val inArchive = SevenZip.openInArchive(null, inStream, object : IArchiveOpenCallback {
            override fun setTotal(files: Long?, bytes: Long?) {
                Log.e(TAG, "setTotal: $files $bytes")
            }

            override fun setCompleted(files: Long?, bytes: Long?) {
                Log.e(TAG, "setCompleted: $files $bytes")
            }
        })

        val itemCount = inArchive.numberOfItems
        for (i in 0 until itemCount) {
            val filePath = inArchive.getStringProperty(i, PropID.PATH)
            val fileSize = inArchive.getStringProperty(i, PropID.SIZE)
            Log.e(TAG, "filePath:$filePath fileSize:$fileSize")
            val result = inArchive.extractSlow(i) {
                Log.e(TAG, "testExtractFile: ${it.size}")
                it.size
            }
            if (result != ExtractOperationResult.OK) {
                throw SevenZipException(result.toString())
            }
        }

        inArchive.close()
        inStream.close()
    }
}