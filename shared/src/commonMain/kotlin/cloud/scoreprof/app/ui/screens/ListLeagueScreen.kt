package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import scoreprof_resources.Res
import scoreprof_resources.points
import scoreprof_resources.win_percentage
import scoreprof_resources.jump_to_top
import scoreprof_resources.find_me
import cloud.scoreprof.app.ui.components.LeagueCard
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.view_models.ListLeagueViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch

@Composable
fun ListLeagueScreen(
    leagueid: String,
    owneruserid: String,
    leaguename: String,
    navController: NavController,
    leagueViewModel: ListLeagueViewModel,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val setupState by setupViewModel.setup.collectAsState()
    var sortBy by remember { mutableStateOf("points") }
    var isViewingTop by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = leagueid, key2 = sortBy, key3 = isViewingTop) {
        leagueViewModel.onEvent(ListLeagueViewModel.LeagueEvent.LoadLeagueTable(
            leagueid = leagueid,
            owneruserid = owneruserid,
            sortBy = sortBy,
            jumpToTop = isViewingTop
        ))
    }

    val leagueTable by leagueViewModel.leagueTable.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Modern Edge-to-Edge fix
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = leaguename,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (isViewingTop || (leagueTable.isNotEmpty() && leagueTable.first().rank != 1)) {
                    TextButton(onClick = {
                        isViewingTop = !isViewingTop
                    }) {
                        Text(
                            text = if (isViewingTop) stringResource(Res.string.find_me) else stringResource(Res.string.jump_to_top),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 8.dp)
                .fillMaxSize()
        ) {
            // Ranking Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(40.dp))
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.points) + if (sortBy == "points") " ↓" else "",
                    modifier = Modifier
                        .width(40.dp)
                        .clickable { 
                            sortBy = "points"
                            isViewingTop = false
                        },
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = if (sortBy == "points") FontWeight.Bold else FontWeight.Normal,
                        color = if (sortBy == "points") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                )
                Text(
                    text = stringResource(Res.string.win_percentage) + if (sortBy == "win_pct") " ↓" else "",
                    modifier = Modifier
                        .width(60.dp)
                        .clickable { 
                            sortBy = "win_pct"
                            isViewingTop = false
                        },
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

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = leagueTable,
                    key = { tableRow -> tableRow.userid }
                ) { leagueTableItem ->
                    if (leagueTableItem != null) {
                        LeagueCard(leagueTableItem)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Optional Ad Banner at the bottom of the table
            if (setupState?.is_ads_removed == false) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    AdBanner(
                        modifier = Modifier.fillMaxWidth(),
                        isMediumRectangle = false,
                        showAds = true
                    )
                }
            }
        }
    }
}
