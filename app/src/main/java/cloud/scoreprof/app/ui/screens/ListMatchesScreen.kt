package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import cloud.scoreprof.app.domain.model.Match
import cloud.scoreprof.app.ui.components.MatchCard
import cloud.scoreprof.app.ui.view_models.ListMatchesViewModel.MatchesUiState
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.component1
import kotlin.collections.component2
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import cloud.scoreprof.app.ui.view_models.ListMatchesViewModel
import kotlinx.coroutines.launch

@Composable
fun ListMatchesScreen(
    competitionid: String,
    competitionName: String,
    navController: NavController,
    matchesViewModel: ListMatchesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val state by matchesViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // This effect listens for events from the ViewModel's eventFlow
    LaunchedEffect(key1 = true) {
        matchesViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ListMatchesViewModel.UiEvent.NavigateUp -> {
                    // When the NavigateUp event is received, call the NavController
                    navController.navigateUp()
                }
                is ListMatchesViewModel.UiEvent.ShowSnackbar -> {
                    // Show the snackbar
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                    // For Material 3:
                    // snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }
    // This effect will listen for events from the ViewModel
    LaunchedEffect(key1 = competitionid) {
        matchesViewModel.onEvent(
            ListMatchesViewModel.MatchEvent.LoadMatchesForCompetition(
                competitionid
            )
        )
    }
    val uiState by matchesViewModel.uiState.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                IconButton(onClick = {
                    matchesViewModel.onEvent(
                        ListMatchesViewModel.MatchEvent.SaveAndNavigateUp
                    )
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                // Title
                Text(
                    text = competitionName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f) // Ensures title takes up available space
                )
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is MatchesUiState.Loading -> {
                    // Show a loading indicator
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is MatchesUiState.Error -> {
                    // Show an error message
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                is MatchesUiState.Success -> {
                    if (state.groupedMatches.isNotEmpty()) {
                        // Launch an effect that will run once when the index is valid.
                        LaunchedEffect(state.firstUpcomingMatchIndex) {
                            if (state.firstUpcomingMatchIndex != -1) {
                                coroutineScope.launch {
                                    // Scroll to the item. The number of headers (dates) needs to be accounted for.
                                    // First, find out how many headers appear before the target item.
                                    val flatList = state.groupedMatches.values.flatten()
                                    val targetMatch = flatList.getOrNull(state.firstUpcomingMatchIndex)
                                    if (targetMatch != null) {
                                        val headersBefore = state.groupedMatches.keys.count { dateHeader ->
                                            dateHeader.isBefore(targetMatch.kickoff.toLocalDate())
                                        }
                                        // The final index is the item's index plus the number of headers before it.
                                        listState.animateScrollToItem(index = state.firstUpcomingMatchIndex + headersBefore)
                                    }
                                }
                            }
                        }

                        MatchesList(
                            groupedMatches = state.groupedMatches,
                            matchesViewModel = matchesViewModel,
                            lazyListState = listState // Pass the state down
                        )
                    } else {
                        // Show a message for when there are no matches
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.no_upcoming_matches),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            /*Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.season_break_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )*/
                        }

                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MatchesList(
    groupedMatches: Map<LocalDate, List<Match>>,
    matchesViewModel: ListMatchesViewModel,
    lazyListState: LazyListState
) {
    // ... your existing LazyColumn and Column code goes here ...
    Column(
        modifier = Modifier
            .padding(contentPadding())
            .padding(horizontal = 8.dp)
        //.fillMaxWidth() // for the column to take up the entire size of the screen
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.competition_screen_text),
            style = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        LazyColumn(state = lazyListState) {
            groupedMatches.forEach { (date, matchesOnDate) ->

                // 1. Create a sticky header for the date
                stickyHeader {
                    Text(
                        text = date.format(formatter),
                        modifier = Modifier
                            .fillMaxWidth()
                            // Add a background to hide items that scroll behind the header
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // 2. Add the list of matches for that specific date
                items(matchesOnDate) { match ->
                    MatchCard(
                        match,
                        onPredictionClick = { competitor1selected, competitor2selected ->
                            matchesViewModel.onEvent(
                                ListMatchesViewModel.MatchEvent.OnPredictionMade(
                                    match = match,
                                    competitor1selected = competitor1selected,
                                    competitor2selected = competitor2selected
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

fun ZonedDateTimeToLocalDate(utcDateTime: ZonedDateTime): LocalDate {
    // 1. Get the system's default time zone ID.
    val localZoneId = ZoneId.systemDefault()

    // 2. Convert the ZonedDateTime from UTC to the local time zone.
    val localDateTime = utcDateTime.withZoneSameInstant(localZoneId)

    // 3. Extract and return the LocalDate part.
    return localDateTime.toLocalDate()
}
