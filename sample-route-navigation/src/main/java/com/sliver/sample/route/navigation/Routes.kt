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
import com.sliver.sample.route.navigation.features.profile.ProfileActivity
import com.sliver.sample.route.navigation.features.settings.SettingsFragment
import kotlinx.serialization.Serializable

sealed interface Root {
    @Serializable
    data class Main(val name: String = "Home") : Root

    @Serializable
    data class Settings(val name: String = "Setting") : Root

    @Serializable
    data class Profile(val name: String = "Profile") : Root

    @Serializable
    data class Loading(val name: String = "Loading") : Root
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
        activity<Root.Profile> {
            activityClass = ProfileActivity::class
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