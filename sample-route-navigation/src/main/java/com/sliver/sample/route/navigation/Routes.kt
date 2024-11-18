package com.sliver.sample.route.navigation

import androidx.navigation.NavController
import androidx.navigation.activity
import androidx.navigation.createGraph
import androidx.navigation.fragment.dialog
import androidx.navigation.fragment.fragment
import com.sliver.sample.route.navigation.features.loading.LoadingDialog
import com.sliver.sample.route.navigation.features.main.MainFragment
import com.sliver.sample.route.navigation.features.main.fragments.HomeFragment
import com.sliver.sample.route.navigation.features.main.fragments.MineFragment
import com.sliver.sample.route.navigation.features.main.fragments.StarFragment
import com.sliver.sample.route.navigation.features.settings.SettingsFragment
import com.sliver.sample.route.navigation.features.webpage.WebPageActivity
import kotlinx.serialization.Serializable

sealed interface Root {
    @Serializable
    data class Main(val name: String = "Home") : Root

    @Serializable
    data class Settings(val name: String = "Setting") : Root

    @Serializable
    data class Loading(val name: String = "Loading") : Root

    @Serializable
    data class WebPage(val url: String) : Root
}

sealed interface Main {
    @Serializable
    data class Home(val name: String = "Home") : Main

    @Serializable
    data class Star(val name: String = "Star") : Main

    @Serializable
    data class Mine(val name: String = "Mine") : Main
}

fun NavController.setupMainActivityNavGraph() {
    graph = createGraph(Root.Main()) {
        fragment<MainFragment, Root.Main>()
        fragment<SettingsFragment, Root.Settings>()
        activity<Root.WebPage> {
            activityClass = WebPageActivity::class
        }
        dialog<LoadingDialog, Root.Loading>()
    }
}

fun NavController.setupMainFragmentNavGraph() {
    graph = createGraph(Main.Home()) {
        fragment<HomeFragment, Main.Home>()
        fragment<StarFragment, Main.Star>()
        fragment<MineFragment, Main.Mine>()
    }
}


// Navigation Dsl使用:
// Both Dsl and xml: Navigator、NavDestination  Xml: NavAction、NavDirections

// 注意点:
// 使用Kotlin Dsl构造NavGraph时，不再使用NavAction和NavDirections
// 使用Kotlin Dsl构造NavGraph时，NavigationUI无法直接绑定类似BottomNavigationView的控件
// 使用Kotlin Dsl构造NavGraph时，无法使用SafeArg插件，无法使用by navArgs()
// 使用Kotlin Dsl构造NavGraph时，无法给Activity设置DeepLink（从外部浏览器启动）
// Navigation并未提供Activity.startActivityForResult的封装
// 导航到Activity时无法获取NavController

// 一些常见问题的解决方案:
// 向上一个Fragment或Activity传递数据通用方式:
// 直接导航到上一个页面并附加一些导航策略使得旧页面不再保存
// 向上一个Fragment传递数据:
// https://developer.android.com/guide/navigation/use-graph/programmatic?hl=zh-cn#returning_a_result
// 向上一个Activity传递数据:
// 这个页面单独使用Activity.startActivityForResul或registerActivityResult