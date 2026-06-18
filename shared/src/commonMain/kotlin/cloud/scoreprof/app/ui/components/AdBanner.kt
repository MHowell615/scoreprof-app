package cloud.scoreprof.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AdBanner(
    modifier: Modifier = Modifier,
    isMediumRectangle: Boolean = false,
    showAds: Boolean = true
)
