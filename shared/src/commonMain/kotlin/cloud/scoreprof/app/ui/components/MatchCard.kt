package cloud.scoreprof.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.scoreprof.app.domain.model.Match
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.theme.button_background_disabled
import cloud.scoreprof.app.ui.theme.font_disabled
import cloud.scoreprof.app.ui.theme.prediction_background
import cloud.scoreprof.app.ui.theme.prediction_incorrect
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import org.jetbrains.compose.resources.stringResource
import scoreprof_resources.Res
import scoreprof_resources.vs

@Composable
fun MatchCard(
    match: Match,
    onPredictionClick: (competitor1selected: Boolean, competitor2selected: Boolean) -> Unit
) {
    var competitor1selected by rememberSaveable { mutableStateOf(match.competitor1selected) }
    var competitor2selected by rememberSaveable { mutableStateOf(match.competitor2selected) }

    // Determine if the prediction period is over
    val nowInSeconds = Clock.System.now().epochSeconds
    val predictionDeadlineInSeconds = match.kickoff.epochSeconds - 60
    val isPredictionLocked = predictionDeadlineInSeconds < nowInSeconds

    // Main card color and font color depend only on whether predictions are locked
    val cardBackgroundColor =
        if (isPredictionLocked) button_background_disabled else button_background
    val fontColor = if (isPredictionLocked) font_disabled else MaterialTheme.colorScheme.primary

    // This function will determine the background for each competitor selection
    fun getSelectionBackgroundColor(): Color {
        // If the winner is not known yet, always show the standard prediction background
        if (match.winner == null) {
            return prediction_background
        }

        // The winner is known, so let's validate the prediction
        val userPredictedWinner = when {
            competitor1selected && competitor2selected -> 0 // User predicted a draw
            competitor1selected -> 1                       // User predicted Competitor 1 win
            competitor2selected -> 2                       // User predicted Competitor 2 win
            else -> -1                                     // No prediction made
        }

        return if (userPredictedWinner == match.winner) {
            prediction_background // Correct!
        } else {
            prediction_incorrect  // Incorrect
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = cardBackgroundColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(16.dp)
        ) {

            val competitor1BackgroundColor =
                if (competitor1selected) getSelectionBackgroundColor() else Color.Transparent
            val competitor2BackgroundColor =
                if (competitor2selected) getSelectionBackgroundColor() else Color.Transparent

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Competitor 1
                    Text(
                        text = match.competitor1,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(competitor1BackgroundColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = {
                                    if (!isPredictionLocked) {
                                        competitor1selected = !competitor1selected
                                        onPredictionClick(competitor1selected, competitor2selected)
                                    }
                                }
                            )
                            .padding(4.dp),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Score or "vs" Text
                    if (match.score1 != null && match.score2 != null) {
                        Text(
                            text = "${match.score1} - ${match.score2}",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.vs),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = fontColor,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    // Competitor 2
                    Text(
                        text = match.competitor2,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(competitor2BackgroundColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = {
                                    if (!isPredictionLocked) {
                                        competitor2selected = !competitor2selected
                                        onPredictionClick(competitor1selected, competitor2selected)
                                    }
                                }
                            )
                            .padding(4.dp),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        match.venue?.let {
                            Text(
                                text = it,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxWidth(0.35f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        match.supplement?.let {
                            Text(
                                text = it,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fontColor,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.align(Alignment.Center),
                                maxLines = 1
                            )
                        }

                        // 3. Kickoff Time
                        val systemTimeZone = TimeZone.currentSystemDefault()
                        val localKickoff = match.kickoff.toLocalDateTime(systemTimeZone)
                        val timeString = "${localKickoff.hour.toString().padStart(2, '0')}:${localKickoff.minute.toString().padStart(2, '0')}"

                        Text(
                            text = timeString,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End
                            ),
                            modifier = Modifier.align(Alignment.CenterEnd),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
