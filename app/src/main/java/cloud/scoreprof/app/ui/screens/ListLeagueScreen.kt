package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.LeagueCard
import cloud.scoreprof.app.ui.view_models.ListLeagueViewModel
import java.util.UUID

@Composable
fun ListLeagueScreen(
    //id: Int,
    leagueid: String,
    owneruserid: UUID,
    leaguename: String,
    navController: NavController,
    leagueViewModel: ListLeagueViewModel,
    modifier: Modifier = Modifier
) {
    // (1) Define the sort state. Default to "points"
    var sortBy by remember { mutableStateOf("points") }

    // This effect will listen for events from the ViewModel
    LaunchedEffect(key1 = leagueid, key2 = sortBy) {
        leagueViewModel.onEvent(ListLeagueViewModel.LeagueEvent.LoadLeagueTable(
            leagueid = leagueid,
            owneruserid = owneruserid,
            sortBy = sortBy
            )
        )
    }

    // Observe the state from the ViewModel
    val originalLeagueList by leagueViewModel.leagueTable.collectAsState()
    val leagueTable by leagueViewModel.leagueTable.collectAsState()

    Scaffold(
        topBar = {
            // A custom top bar using a Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Standard height for a top app bar
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Navigation Icon
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                // Title
                Text(
                    text = stringResource(id = R.string.league) + ": " + leaguename,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f) // Ensures title takes up available space
                )
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 8.dp)
            //.fillMaxWidth() // for the column to take up the entire size of the screen
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spacer to align with the name in the card
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(id = R.string.points) + if (sortBy == "points") " ↓" else "",
                    modifier = Modifier
                        .width(60.dp)
                        .clickable { sortBy = "points" },
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = if (sortBy == "points") FontWeight.Bold else FontWeight.Normal,
                        color = if (sortBy == "points") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                )
                Text(
                    text = stringResource(id = R.string.win_percentage) + if (sortBy == "win_pct") " ↓" else "",
                    modifier = Modifier
                        .width(80.dp)
                        .clickable { sortBy = "win_pct" },
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = if (sortBy == "win_pct") FontWeight.Bold else FontWeight.Normal,
                        color = if (sortBy == "win_pct") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            //val originalLeagueList by leagueViewModel.leagueTable.collectAsState()
            //val leagueTable = originalLeagueList.filterNotNull()
            LazyColumn {
                items(
                    items = leagueTable,
                    key = { tableRow ->
                        tableRow.userid
                    }
                ) { leagueTableItem ->
                    if (leagueTableItem != null) {
                        LeagueCard(leagueTableItem)
                    } else {
                        Text("Invalid item data")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

            }
        }
    }
}