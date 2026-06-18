package cloud.scoreprof.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AdBanner(
    modifier: Modifier,
    isMediumRectangle: Boolean,
    showAds: Boolean
) {
    // For now, return an empty Box on iOS
    Box(modifier = modifier)
}
