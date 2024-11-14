package com.sliver.sample.di.hilt.service

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

interface BaiduApiService {

    @GET("https://www.baidu.com/")
    fun getHomePage(): Call<ResponseBody>
}