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
fun AdBanner(
    modifier: Modifier = Modifier,
    isMediumRectangle: Boolean = false,
    showAds: Boolean = true
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
                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID
                val adRequest = AdRequest.Builder()
                    .addKeyword("Sport")
                    .addKeyword("Football")
                    .addKeyword("Soccer")
                    .addKeyword("Rugby")
                    .addKeyword("Match Scores")
                    .addKeyword("Tournament")
                    .build()
                loadAd(adRequest)
            }
        },
        update = { /* No update logic needed */ }
    )
}
