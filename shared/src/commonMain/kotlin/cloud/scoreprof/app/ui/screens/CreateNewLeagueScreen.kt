package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import scoreprof_resources.Res
import scoreprof_resources.share_league_msg
import scoreprof_resources.copy_to_clipboard
import scoreprof_resources.create_new_league
import scoreprof_resources.competitions
import scoreprof_resources.league_name
import scoreprof_resources.league_created
import cloud.scoreprof.app.Platform
import cloud.scoreprof.app.ui.theme.sub_dropdown_background
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.utils.SelectableRowWithCheckboxes
import cloud.scoreprof.app.ui.view_models.CreateLeagueViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@Composable
fun CreateNewLeagueScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    savedStateHandle: SavedStateHandle,
    viewModel: CreateLeagueViewModel = koinViewModel()
) {
    val setupState by setupViewModel.setup.collectAsState()
    val competitionsState by setupViewModel.competitions.collectAsState()
    val selectedCompetition by viewModel.selectedCompetition.collectAsState()
    val leagueName by viewModel.leagueName.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val platform = koinInject<Platform>()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var generatedLeagueCode by remember { mutableStateOf("") }

    val shareLeagueMsg = stringResource(Res.string.share_league_msg, generatedLeagueCode)
    val copyToClipboardMsg = stringResource(Res.string.copy_to_clipboard)

    val groupedData = remember(competitionsState) {
        competitionsState
            .filter { it.isSelected }
            .groupBy { it.item.sport_type ?: "Other" }
            .mapValues { sportEntry ->
                sportEntry.value.groupBy { it.item.region ?: "International" }
                    .toList().sortedBy { it.first }.toMap()
            }.toList().sortedBy { it.first }.toMap()
    }

    val expandedSports = remember { mutableStateMapOf<String, Boolean>() }
    val expandedRegions = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is CreateLeagueViewModel.CreateLeagueResult.Success -> {
                generatedLeagueCode = result.leagueCode
                showSuccessDialog = true
                snackbarHostState.showSnackbar(result.message)
            }
            is CreateLeagueViewModel.CreateLeagueResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.onResultConsumed()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = stringResource(Res.string.create_new_league),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createLeague() },
                containerColor = if (saveResult is CreateLeagueViewModel.CreateLeagueResult.Loading) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                if (saveResult is CreateLeagueViewModel.CreateLeagueResult.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = leagueName,
                        onValueChange = viewModel::onLeagueNameChanged,
                        label = { Text(stringResource(Res.string.league_name)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (setupState?.is_ads_removed == false) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            AdBanner(
                                modifier = Modifier.fillMaxWidth(),
                                isMediumRectangle = false,
                                showAds = true
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = stringResource(Res.string.competitions),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            for ((sportType, regions) in groupedData) {
                item(key = sportType) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            val isSportExpanded = expandedSports[sportType] ?: false
                            Surface(
                                onClick = { expandedSports[sportType] = !isSportExpanded },
                                color = dropdown_background,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sportType.uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(if (isSportExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                                }
                            }

                            if (isSportExpanded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 250.dp)
                                        .verticalScroll(rememberScrollState())
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        for ((regionName, competitionList) in regions) {
                                            val regionKey = "${sportType}_$regionName"
                                            val isRegionExpanded = expandedRegions[regionKey] ?: false

                                            Surface(
                                                onClick = { expandedRegions[regionKey] = !isRegionExpanded },
                                                color = sub_dropdown_background,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            ) {
                                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = regionName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                                    Icon(if (isRegionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(20.dp))
                                                }
                                            }

                                            if (isRegionExpanded) {
                                                for (competition in competitionList) {
                                                    SelectableRowWithCheckboxes(
                                                        item = competition.item,
                                                        name = competition.item.name,
                                                        isSelected = (competition.item.id == selectedCompetition?.id),
                                                        onCheckedChange = { viewModel.onCompetitionSelected(competition.item) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.popBackStack()
            },
            title = {
                Text(text = stringResource(Res.string.league_created), color = MaterialTheme.colorScheme.primary)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Share this code with your friends so they can join:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generatedLeagueCode,
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        platform.shareText(shareLeagueMsg)
                    }
                ) { Text("Share") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(generatedLeagueCode))
                        scope.launch {
                            snackbarHostState.showSnackbar(copyToClipboardMsg)
                        }
                        showSuccessDialog = false
                        navController.popBackStack()
                    }
                ) { Text("Copy & Close") }
            }
        )
    }
}
