package com.sliver.sampledi

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.sliver.sampledi.service.BaiduApiService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

//0.自定义Application，并使用@HiltAndroidApp提供注解处理的入口点
@HiltAndroidApp
class HiltStartupApplication : Application()

//1.提供实例
//1.1 通过@Inject构造函数提供对象
class Logger @Inject constructor() {
    fun log(msg: String) {
        Log.e("Logger", msg)
    }
}

//1.2 使用Bind提供接口实例
interface ImageDataSource {
    fun getImages(): List<String>
}

class ImageLocalDataSource @Inject constructor(
    private val logger: Logger
) : ImageDataSource {
    override fun getImages(): List<String> {
        logger.log("getImages from local")
        return emptyList()
    }
}

class ImageRemoteDataSource @Inject constructor(
    private val logger: Logger
) : ImageDataSource {
    override fun getImages(): List<String> {
        logger.log("getImages from remote")
        return emptyList()
    }
}

@Module
@InstallIn(ActivityComponent::class)
interface BindModule {
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class LocalDataSource

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class RemoteDataSource

    @Binds
    @LocalDataSource
    fun bindLocalDataSource(
        imageLocalDataSource: ImageLocalDataSource
    ): ImageDataSource

    @Binds
    @RemoteDataSource
    fun bindRemoteDataSource(
        imageRemoteDataSource: ImageRemoteDataSource
    ): ImageDataSource
}


//1.3 使用Providers提供实例
@Module
@InstallIn(SingletonComponent::class)
object ProvidesModule {
    @Provides
    @Singleton
    fun provideOkhttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.baidu.com/")
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideBaiduService(retrofit: Retrofit): BaiduApiService {
        return retrofit.create(BaiduApiService::class.java)
    }
}
//1.4 指定Module可见性和实例的生命周期
//见：https://developer.android.google.cn/training/dependency-injection/hilt-android?hl=zh-cn#component-lifetimes
//@InstallIn(SingletonComponent::class)
//@Singleton


//2. 注入实例
//2.1 标记入口点
//Application -> @HiltAndroidApp
//ViewModel -> @HiltViewModel
//Activity、Fragment、View 、Service、BroadcastReceiver-> @AndroidEntryPoint

//2.2 注入实例（构造函数注入、字段注入）

//2.3 注入Context（@ApplicationContext、@ActivityContext）
class NetworkRepository @Inject constructor(
    @ActivityContext private val context: Context,
)


//3. 搭配ViewModel及其他库使用
//ListenableWorker @HiltWorker
//Views ViewModel @HiltViewModel
//Compose ViewModel @hiltViewModel
@HiltViewModel
class TestViewModel @Inject constructor(
    private val logger: Logger
) : ViewModel() {
    fun testLog() {
        logger.log("TestViewModel")
    }
}

@AndroidEntryPoint
class TestActivity : AppCompatActivity() {
    private val viewModel by viewModels<TestViewModel>()
}