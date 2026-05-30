package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.components.MatchCard
import cloud.scoreprof.app.ui.view_models.ListMatchesViewModel.MatchesUiState
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
    setupViewModel: ListSetupViewModel = hiltViewModel(), // Added to check premium status
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by matchesViewModel.uiState.collectAsState()
    val setupState by setupViewModel.setup.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        matchesViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ListMatchesViewModel.UiEvent.NavigateUp -> navController.navigateUp()
                is ListMatchesViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    LaunchedEffect(key1 = competitionid) {
        matchesViewModel.onEvent(ListMatchesViewModel.MatchEvent.LoadMatchesForCompetition(competitionid))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                //color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = {
                        matchesViewModel.onEvent(ListMatchesViewModel.MatchEvent.SaveAndNavigateUp)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = competitionName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            lineHeight = 24.sp
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is MatchesUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is MatchesUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { matchesViewModel.retryLoading() }) {
                                Text(stringResource(id = R.string.retry))
                            }
                        }
                    }

                    is MatchesUiState.Success -> {
                        if (state.groupedMatches.isNotEmpty()) {
                            LaunchedEffect(state.firstUpcomingMatchIndex) {
                                if (state.firstUpcomingMatchIndex != -1) {
                                    coroutineScope.launch {
                                        val flatList = state.groupedMatches.values.flatten()
                                        val targetMatch = flatList.getOrNull(state.firstUpcomingMatchIndex)
                                        if (targetMatch != null) {
                                            val headersBefore = state.groupedMatches.keys.count { it.isBefore(targetMatch.kickoff.toLocalDate()) }
                                            listState.animateScrollToItem(index = state.firstUpcomingMatchIndex + headersBefore)
                                        }
                                    }
                                }
                            }
                            MatchesList(state.groupedMatches, matchesViewModel, listState)
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = stringResource(id = R.string.no_upcoming_matches), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            // Ad Banner for non-premium users
            if (setupState?.is_ads_removed == false) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    AdBanner(modifier = Modifier.fillMaxWidth(), isMediumRectangle = false, showAds = true)
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
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.competition_screen_text),
            style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        LazyColumn(state = lazyListState) {
            groupedMatches.forEach { (date, matchesOnDate) ->
                stickyHeader {
                    Text(
                        text = date.format(formatter),
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp),
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                }
                items(matchesOnDate) { match ->
                    MatchCard(match, onPredictionClick = { c1, c2 ->
                        matchesViewModel.onEvent(ListMatchesViewModel.MatchEvent.OnPredictionMade(match, c1, c2))
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
