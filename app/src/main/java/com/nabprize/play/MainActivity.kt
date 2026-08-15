package com.nabprize.play

import android.os.Bundle
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.nabprize.play.ui.navigation.NabPrizeNavGraph
import com.nabprize.play.ui.theme.NabPrizeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep gameplay and ads in portrait even if the device's rotation sensor is active.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK early
        val testDeviceIds = listOf("38B3ADE7AAC88CF6B868914DD4D75A68")
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        )
        MobileAds.initialize(this) {}

        setContent {
            NabPrizeTheme {
                NabPrizeNavGraph()
            }
        }
    }
}
