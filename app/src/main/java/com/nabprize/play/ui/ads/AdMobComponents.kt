package com.nabprize.play.ui.ads

import android.app.Activity
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.nabprize.play.ui.theme.rememberResponsiveMetrics

object AdIds {
    const val BANNER       = "ca-app-pub-3940256099942544/6300978111"
    const val NATIVE       = "ca-app-pub-3940256099942544/2247696110"
    const val REWARDED     = "ca-app-pub-3940256099942544/5224354917"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
}

private fun adRequest(): AdRequest = AdRequest.Builder().build()
private val testDeviceIds = listOf("38B3ADE7AAC88CF6B868914DD4D75A68")

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        )
        MobileAds.initialize(context) { initialized = true }
    }
    val adView = remember(initialized) {
        AdView(context).apply {
            adUnitId = AdIds.BANNER
            setAdSize(AdSize.BANNER)
            loadAd(adRequest())
        }
    }
    DisposableEffect(Unit) { onDispose { adView.destroy() } }
    AndroidView(factory = { adView }, modifier = modifier.fillMaxWidth().height(50.dp))
}

@Composable
fun AdMobNativeAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val metrics = rememberResponsiveMetrics()
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adFailed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        )
        val loader = AdLoader.Builder(context, AdIds.NATIVE)
            .forNativeAd { ad -> nativeAd = ad }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {}
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adFailed = true
                }
            })
            .build()
        loader.loadAd(adRequest())
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val currentAd = nativeAd
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (metrics.isCompact) 200.dp else 230.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE8E0D8), RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        if (currentAd != null && !adFailed) {
            AndroidView(
            factory = { ctx ->
                val adView = NativeAdView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val headline = TextView(ctx).apply {
                    text = currentAd.headline ?: ""
                    setTextColor(android.graphics.Color.BLACK)
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                val body = TextView(ctx).apply {
                    text = currentAd.body ?: ""
                    setTextColor(android.graphics.Color.DKGRAY)
                    textSize = 13f
                }

                val advertiser = TextView(ctx).apply {
                    text = currentAd.advertiser ?: "Sponsored"
                    setTextColor(android.graphics.Color.GRAY)
                    textSize = 11f
                }

                val cta = TextView(ctx).apply {
                    text = currentAd.callToAction ?: "Learn More"
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 12f
                    setPadding(32, 12, 32, 12)
                    setBackgroundColor(0xFF1A73E8.toInt())
                }

                // Keep the media asset inside the fixed Compose ad slot. The old 240/300px
                // media height exceeded the 200/230dp parent on smaller phones.
                val mediaView = MediaView(ctx).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        if (metrics.isCompact) 92 else 112
                    )
                }

                val iconView = android.widget.ImageView(ctx).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(48, 48)
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                }
                currentAd.icon?.drawable?.let { iconView.setImageDrawable(it) }

                val topRow = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(16, 16, 16, 0)
                }
                topRow.addView(iconView)
                val textCol = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(12, 0, 0, 0)
                }
                textCol.addView(headline)
                textCol.addView(advertiser)
                topRow.addView(textCol)

                val mainLayout = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                }
                mainLayout.addView(topRow)
                mainLayout.addView(body, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 4 })
                mainLayout.addView(mediaView)
                mainLayout.addView(cta, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8; bottomMargin = 16; marginStart = 16 })

                adView.addView(mainLayout)

                // Register assets only after every asset view is inside this NativeAdView.
                // This prevents the Google Mobile Ads "asset outside native ad view" warning.
                adView.headlineView = headline
                adView.bodyView = body
                adView.advertiserView = advertiser
                adView.callToActionView = cta
                adView.mediaView = mediaView
                adView.iconView = iconView
                adView.setNativeAd(currentAd)
                adView
            },
            update = { adView ->
                (adView as NativeAdView).setNativeAd(currentAd)
            },
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp)
            )
        } else {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Ad will show here", color = Color(0xFF8A8178), fontSize = 13.sp)
                Text("Sponsored space", color = Color(0xFFB0A79E), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun rememberRewardedAdState(): RewardedAdState {
    val context = LocalContext.current
    val state = remember { RewardedAdState() }
    LaunchedEffect(Unit) {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        )
        MobileAds.initialize(context) {}
        state.load(context)
    }
    return state
}

class RewardedAdState {
    var ad by mutableStateOf<RewardedAd?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isRewarded by mutableStateOf(false)
        private set

    fun load(context: Context) {
        if (isLoading || ad != null) return
        isLoading = true
        RewardedAd.load(context, AdIds.REWARDED, adRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loadedAd: RewardedAd) {
                    this@RewardedAdState.ad = loadedAd
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    this@RewardedAdState.ad = null
                    isLoading = false
                }
            })
    }

    fun show(
        activity: Activity,
        onRewarded: () -> Unit = {},
        onUnavailable: () -> Unit = {},
        onDismissedWithoutReward: () -> Unit = {}
    ) {
        val currentAd = ad
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    val earnedReward = isRewarded
                    ad = null
                    isRewarded = false
                    if (!earnedReward) onDismissedWithoutReward()
                    load(activity)
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    ad = null
                    isRewarded = false
                    onUnavailable()
                    load(activity)
                }
            }
            currentAd.show(activity) {
                isRewarded = true
                onRewarded()
            }
        } else {
            // Caller may use the PRD fallback (interstitial) when no rewarded is ready.
            onUnavailable()
            load(activity)
        }
    }
}

@Composable
fun rememberInterstitialAdState(): InterstitialAdState {
    val context = LocalContext.current
    val state = remember { InterstitialAdState() }
    LaunchedEffect(Unit) {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        )
        MobileAds.initialize(context) {}
        state.load(context)
    }
    return state
}

class InterstitialAdState {
    var ad by mutableStateOf<InterstitialAd?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    fun load(context: Context) {
        isLoading = true
        InterstitialAd.load(context, AdIds.INTERSTITIAL, adRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    ad = interstitialAd
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    isLoading = false
                }
            })
    }

    fun show(activity: Activity, onDismissed: () -> Unit = {}) {
        val currentAd = ad
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    ad = null
                    onDismissed()
                    load(activity)
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    ad = null
                    onDismissed()
                    load(activity)
                }
            }
            currentAd.show(activity)
        } else {
            // If ad is not loaded, still proceed with the action
            onDismissed()
            load(activity)
        }
    }
}
