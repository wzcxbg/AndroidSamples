package com.sliver.samples

import android.content.Intent
import android.util.Log
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.sliver.samples.base.BaseActivity
import com.sliver.samples.custom.FriendListAdapter
import com.sliver.samples.databinding.ActivityMainBinding
import com.sliver.samples.screencapture.TestScreenCaptureActivity

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

    companion object{
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
        binding.terminal.setOnClickListener {
            screenCapture()
//            controller.execute("ifconfig")
//            controller.execute("ffmpeg")
//            Thread.sleep(3000)
//            controller.execute("input tap 540 1000")
//            controller.shutdown()
        }
    }
}