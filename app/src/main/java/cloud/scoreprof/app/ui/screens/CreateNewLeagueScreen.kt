package cloud.scoreprof.app.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import cloud.scoreprof.app.ui.utils.SelectableRowWithCheckboxes
import cloud.scoreprof.app.ui.view_models.CreateLeagueViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.theme.sub_dropdown_background
import java.util.UUID

@Composable
fun CreateNewLeagueScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    savedStateHandle: SavedStateHandle,
    viewModel: CreateLeagueViewModel = hiltViewModel()
) {
    val setupState by setupViewModel.setup.collectAsState()
    val competitionsState by setupViewModel.competitions.collectAsState()
    val selectedCompetition by viewModel.selectedCompetition.collectAsState()
    val leagueName by viewModel.leagueName.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    
    val context = LocalContext.current
    var showSuccessDialog by remember { mutableStateOf(false) }
    var generatedLeagueCode by remember { mutableStateOf("") }

    val shareLeagueMsg = stringResource(id = R.string.share_league_msg)
    val copyToClipboardMsg = stringResource(id = R.string.copy_to_clipboard)

    val groupedData = remember(competitionsState) {
        competitionsState
            .filter { it.isSelected }
            .groupBy { it.item.sport_type ?: "Other" }
            .mapValues { sportEntry ->
                sportEntry.value.groupBy { it.item.region ?: "International" }
                    .toSortedMap()
            }.toSortedMap()
    }

    val expandedSports = remember { mutableStateMapOf<String, Boolean>() }
    val expandedRegions = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is CreateLeagueViewModel.CreateLeagueResult.Success -> {
                generatedLeagueCode = result.leagueCode
                showSuccessDialog = true
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
            is CreateLeagueViewModel.CreateLeagueResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.onResultConsumed()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface, // Gives it a solid background
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = stringResource(id = R.string.create_new_league),
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
                        label = { Text(stringResource(id = R.string.league_name)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Conditionally show/hide AdBanner
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
                        text = stringResource(id = R.string.competitions),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            groupedData.forEach { (sportType, regions) ->
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
                                        regions.forEach { (regionName, competitionList) ->
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
                                                competitionList.forEach { competition ->
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
                Text(text = stringResource(R.string.league_created), color = MaterialTheme.colorScheme.primary)
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
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, shareLeagueMsg)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                    }
                ) { Text("Share") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("League Code", generatedLeagueCode))
                        Toast.makeText(context, copyToClipboardMsg, Toast.LENGTH_SHORT).show()
                        showSuccessDialog = false
                        navController.popBackStack()
                    }
                ) { Text("Copy & Close") }
            }
        )
    }
}
