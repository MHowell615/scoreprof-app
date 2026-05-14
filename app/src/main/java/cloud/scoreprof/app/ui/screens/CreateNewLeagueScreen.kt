package cloud.scoreprof.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@Composable
fun CreateNewLeagueScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    savedStateHandle: SavedStateHandle,
    viewModel: CreateLeagueViewModel = hiltViewModel()
) {
    // Get the application context to access the repository
    //val application = LocalContext.current.applicationContext as ScoreProfApplication

    // State from SetupViewModel (for the list of available competitions)
    val availableCompetitions by viewModel.availableCompetitions.collectAsState()
    val setupState by setupViewModel.setup.collectAsState()
    val competitionsState by setupViewModel.competitions.collectAsState()

    // State from our new CreateLeagueViewModel (for the league being created)
    val selectedCompetition by viewModel.selectedCompetition.collectAsState()
    val leagueName by viewModel.leagueName.collectAsState()

    val inviteEmailInput by viewModel.inviteEmailInput.collectAsState()
    val invitedEmails by viewModel.invitedEmails.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val inviteStatus by viewModel.saveResult.collectAsState()
    val context = LocalContext.current
    var showSuccessDialog by remember { mutableStateOf(false) }
    var generatedLeagueCode by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboard.current

    val groupedData = remember(competitionsState) {
        competitionsState
            .filter { it.isSelected }
            .groupBy { it.item.sport_type ?: "Other" }
            .mapValues { sportEntry ->
                // Group the competitions within each sport by Country/Region
                sportEntry.value.groupBy { it.item.region ?: "International" }
                    .toSortedMap()
            }.toSortedMap()
    }

    val expandedSports = remember { mutableStateMapOf<String, Boolean>() }
    val expandedRegions = remember { mutableStateMapOf<String, Boolean>() }

    // LaunchedEffect for showing Toasts for invite status (e.g., "Email added")
    LaunchedEffect(inviteStatus) {
        inviteStatus.let { message ->
            viewModel.onInviteStatusConsumed()
        }
    }

    LaunchedEffect(competitionsState) {
        println("TEST: competitionsState: $competitionsState")
        println("TEST: : setupState: $setupState")
    }

    LaunchedEffect(saveResult) {
        // Use a local variable for the result inside the 'when' for cleaner code
        when (val result = saveResult) {
            is CreateLeagueViewModel.CreateLeagueResult.Success -> {
                generatedLeagueCode = result.leagueCode
                showSuccessDialog = true
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }

            is CreateLeagueViewModel.CreateLeagueResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.onResultConsumed() // Reset the error state
            }

            else -> {}
        }
    }



    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    text = stringResource(id = R.string.create_new_league),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }, // <-- Comma separates topBar from the next parameter
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.createLeague()
                    //navController.popBackStack()
                },
                containerColor = if (
                    saveResult is CreateLeagueViewModel.CreateLeagueResult.Loading
                ) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                if (saveResult is CreateLeagueViewModel.CreateLeagueResult.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Save, // Use the standard Save icon
                        contentDescription = stringResource(R.string.create_league) // Content description for accessibility
                    )
                }
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                OutlinedTextField(
                    // Connect to the ViewModel's state and event
                    value = leagueName,
                    onValueChange = viewModel::onLeagueNameChanged,
                    label = { Text(stringResource(id = R.string.league_name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(vertical = 8.dp)
                ) {
                    AdBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        isMediumRectangle = false
                    )
                }
            }
            item {
                Text(
                    text = stringResource(id = R.string.competitions),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            // Use the full list from setupViewModel to display all options
            groupedData.forEach { (sportType, regions) ->
                // --- LEVEL 1: SPORT HEADER ---
                item(key = "sport_$sportType") {
                    val isSportExpanded = expandedSports[sportType] ?: false
                    Surface(
                        onClick = { expandedSports[sportType] = !isSportExpanded },
                        color = dropdown_background,
                        contentColor = MaterialTheme.colorScheme.primary, //Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sportType.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isSportExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (expandedSports[sportType] == true) {
                    regions.forEach { (regionName, competitionList) ->
                        // --- LEVEL 2: REGION/COUNTRY HEADER ---
                        item(key = "region_${sportType}_$regionName") {
                            val isRegionExpanded =
                                expandedRegions["${sportType}_$regionName"] ?: false
                            Surface(
                                onClick = {
                                    expandedRegions["${sportType}_$regionName"] = !isRegionExpanded
                                },
                                color = sub_dropdown_background,
                                contentColor = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = regionName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isRegionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // --- LEVEL 3: COMPETITION BUTTONS ---
                        if (expandedRegions["${sportType}_$regionName"] == true) {
                            items(
                                competitionList,
                                key = { "comp_${it.item.competitionid}" }) { competition ->
                                SelectableRowWithCheckboxes(
                                    modifier = Modifier.animateItem(),
                                    item = competition.item,
                                    name = competition.item.name,
                                    isSelected = (competition.item.id == selectedCompetition?.id),
                                    onCheckedChange = {
                                        viewModel.onCompetitionSelected(competition.item)
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            /*item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(vertical = 8.dp)
                ) {
                    AdBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        isMediumRectangle = false
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.invite_others),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Input field for adding emails
            item {
                //Text(stringResource(id = R.string.invite_users), style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inviteEmailInput,
                        onValueChange = viewModel::onInviteEmailChanged,
                        label = { Text(stringResource(id = R.string.invite_users)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    // Button to ADD the email to the list
                    Button(
                        onClick = viewModel::onEmailAdded,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(stringResource(id = R.string.add))
                    }
                }
            }
            // List of invited emails
            items(invitedEmails) { email ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = email,
                        modifier = Modifier
                            .weight(1f)
                    )
                    IconButton(
                        onClick = {
                            viewModel.onEmailRemoved(email)
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Email")
                    }
                }
                //Divider()
            }

            // Add padding at the bottom of the list to ensure the FAB doesn't hide content
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }*/
        }
    }


    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.popBackStack() // Go back only when dismissed
            },
            title = {
                Text(
                    text = stringResource(R.string.league_created ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Share this code with your friends so they can join:")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Display the League Code prominently
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generatedLeagueCode,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Trigger Android Share Sheet
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                context.getString(R.string.share_league_msg)
                                    /*"Join my ScoreProf league! Code: $generatedLeagueCode\n" +
                                        "Download at: https://scoreprof.cloud"*/
                            )
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Use the Android System Clipboard Service
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("League Code", generatedLeagueCode)
                        clipboard.setPrimaryClip(clip)

                        // Show a small feedback to the user
                        Toast.makeText(context, context.getString(R.string.copy_to_clipboard), Toast.LENGTH_SHORT).show()

                        showSuccessDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Copy & Close")
                }
            }
        )
    }
}





