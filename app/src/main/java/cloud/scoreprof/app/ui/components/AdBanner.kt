package cloud.scoreprof.app.ui.components

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
    isMediumRectangle: Boolean = false
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                // Set size based on the parameter
                setAdSize(
                    if (isMediumRectangle)
                        AdSize.MEDIUM_RECTANGLE
                    else
                        AdSize.LARGE_BANNER
                )
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                val adRequest = AdRequest.Builder()
                    .addKeyword("Sport")
                    .addKeyword("Football")
                    .addKeyword("Soccer")
                    .addKeyword("Rugby")
                    .addKeyword("Sports Gear")
                    .addKeyword("Sports Betting")
                    .addKeyword("Match Scores")
                    .addKeyword("Tournament")
                    .addKeyword("Sports News")
                    .addKeyword("Sports Betting Odds")
                    .addKeyword("Sports Betting Predictions")
                    .addKeyword("Team Jersey")
                    .addKeyword("Sports Kits")
                    .addKeyword("Sports Apparel")
                    .addKeyword("Sports Accessories")
                    .addKeyword("Sports Shoes")
                    .build()
                loadAd(adRequest)
            }
        },
        update = { adView ->
            // No update logic needed for static rectangle
        }
    )
}