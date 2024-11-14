package com.sliver.sample.di.hilt

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sliver.sample.di.hilt.service.BaiduApiService
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<TestViewModel>()

    @Inject
    @BindModule.RemoteDataSource
    lateinit var imageDataSource: ImageDataSource

    @Inject
    lateinit var baiduApiService: BaiduApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<View>(R.id.main).setOnClickListener {
            baiduApiService.getHomePage().enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(call: Call<ResponseBody?>, resp: Response<ResponseBody?>) {
                    Log.e("TAG", "onResponse: ${resp.body()?.string()}")
                }

                override fun onFailure(call: Call<ResponseBody?>, e: Throwable) {
                    Log.e("TAG", "onFailure: ", e)
                }
            })

            imageDataSource.getImages()

            viewModel.testLog()
        }
    }
}