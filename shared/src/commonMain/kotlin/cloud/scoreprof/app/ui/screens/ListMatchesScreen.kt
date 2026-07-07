package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import scoreprof_resources.Res
import scoreprof_resources.competition_screen_text
import cloud.scoreprof.app.domain.model.Match
import scoreprof_resources.no_upcoming_matches
import scoreprof_resources.retry
import scoreprof_resources.login
import scoreprof_resources.close_btn
import scoreprof_resources.login_required_title
import scoreprof_resources.login_required_msg
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.components.MatchCard
import cloud.scoreprof.app.ui.view_models.ListMatchesViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.*

@Composable
fun ListMatchesScreen(
    competitionid: String,
    competitionName: String,
    navController: NavController,
    matchesViewModel: ListMatchesViewModel = koinViewModel(),
    setupViewModel: ListSetupViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by matchesViewModel.uiState.collectAsState()
    val setupState by setupViewModel.setup.collectAsState()
    val isGuest = setupViewModel.userid == "00000000-0000-0000-0000-000000000000"
    var showLoginDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text(stringResource(Res.string.login_required_title)) },
            text = { Text(stringResource(Res.string.login_required_msg)) },
            confirmButton = {
                Button(onClick = {
                    showLoginDialog = false
                    scope.launch {
                        setupViewModel.logout {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }) {
                    Text(stringResource(Res.string.login))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text(stringResource(Res.string.close_btn))
                }
            }
        )
    }

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
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
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
                    is ListMatchesViewModel.MatchesUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is ListMatchesViewModel.MatchesUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isGuest) {
                                Text(
                                    text = stringResource(Res.string.login_required_msg),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { showLoginDialog = true }) {
                                    Text(stringResource(Res.string.login))
                                }
                            } else {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { matchesViewModel.retryLoading() }) {
                                    Text(stringResource(Res.string.retry))
                                }
                            }
                        }
                    }

                    is ListMatchesViewModel.MatchesUiState.Success -> {
                        if (state.groupedMatches.isNotEmpty()) {
                            LaunchedEffect(state.firstUpcomingMatchIndex) {
                                if (state.firstUpcomingMatchIndex != -1) {
                                    coroutineScope.launch {
                                        val flatList = state.groupedMatches.values.flatten()
                                        val targetMatch = flatList.getOrNull(state.firstUpcomingMatchIndex)
                                        if (targetMatch != null) {
                                            val systemTZ = TimeZone.currentSystemDefault()
                                            val targetDate = targetMatch.kickoff.toLocalDateTime(systemTZ).date
                                            val headersBefore = state.groupedMatches.keys.count { it < targetDate }
                                            listState.animateScrollToItem(index = state.firstUpcomingMatchIndex + headersBefore)
                                        }
                                    }
                                }
                            }
                            MatchesList(state.groupedMatches, matchesViewModel, listState, isGuest) {
                                showLoginDialog = true
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = stringResource(Res.string.no_upcoming_matches), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

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
    lazyListState: LazyListState,
    isGuest: Boolean,
    onGuestAction: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            textAlign = TextAlign.Center,
            text = stringResource(Res.string.competition_screen_text),
            style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(state = lazyListState) {
            groupedMatches.forEach { (date, matchesOnDate) ->
                stickyHeader {
                    val dateString = "${date.dayOfMonth.toString().padStart(2, '0')}-${date.monthNumber.toString().padStart(2, '0')}-${date.year}"
                    Text(
                        text = dateString,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp),
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                }
                items(matchesOnDate) { match ->
                    MatchCard(match, onPredictionClick = { c1, c2 ->
                        if (isGuest) {
                            onGuestAction()
                        } else {
                            matchesViewModel.onEvent(ListMatchesViewModel.MatchEvent.OnPredictionMade(match, c1, c2))
                        }
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
