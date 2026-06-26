package cloud.scoreprof.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
actual fun AdBanner(
    modifier: Modifier,
    isMediumRectangle: Boolean,
    showAds: Boolean
) {
    if (!showAds) {
        // Return an empty box if ads are disabled for this user
        Box(modifier = modifier)
        return
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(if (isMediumRectangle) AdSize.MEDIUM_RECTANGLE else AdSize.LARGE_BANNER)
                adUnitId = "ca-app-pub-8446803289733319/2334549094"
                val adRequest = AdRequest.Builder()
                    .addKeyword("Sport")
                    .addKeyword("Football")
                    .addKeyword("Soccer")
                    .addKeyword("Rugby")
                    .addKeyword("Match Scores")
                    .addKeyword("Tournament")
                    .build()
@Suppress("MissingPermission")
                loadAd(adRequest)
            }
        },
        update = { /* No update logic needed */ }
    )
}
