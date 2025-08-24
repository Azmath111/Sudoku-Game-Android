package com.azmath.sudoku

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdComposable(modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier, factory = { context ->
        val adView = LayoutInflater.from(context).inflate(R.layout.native_ad, null) as NativeAdView
        adView.visibility = View.GONE

        val adLoader = AdLoader.Builder(context, "ca-app-pub-7087498886582308/1319021685")
            .forNativeAd { ad: NativeAd ->
                populateNativeAdView(ad, adView)
                adView.visibility = View.VISIBLE
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
        adView
    })
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)
    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)

    adView.headlineView = headlineView
    adView.bodyView = bodyView
    adView.callToActionView = callToActionView
    adView.mediaView = mediaView

    headlineView.text = nativeAd.headline
    bodyView.text = nativeAd.body
    callToActionView.text = nativeAd.callToAction
    mediaView.setMediaContent(nativeAd.mediaContent)

    adView.setNativeAd(nativeAd)
}