package cloud.scoreprof.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.scoreprof.app.ui.view_models.CompetitionVM
import cloud.scoreprof.app.ui.theme.button_background

@Composable
fun CompetitionCard(competition: CompetitionVM, modifier: Modifier ?= null) {
    Box(
        modifier = Modifier
            //.border(width = 1.dp, color = Color.Red)
            .fillMaxWidth() // for the border of each card to fill the width of the screen
            .background(
                color = button_background,
                //color = match.backgroundColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                //setupViewModel.getDisplayName(competition),
                "",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .padding(4.dp),
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}