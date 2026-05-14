package cloud.scoreprof.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.theme.sub_dropdown_background
import cloud.scoreprof.app.ui.utils.SelectableRowWithCheckboxes
import cloud.scoreprof.app.ui.view_models.EditLeagueViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SetupEditLeagueScreen(
    navController: NavHostController,
    leagueid: String,
    owneruserid: UUID,
    userid: UUID,
    savedStateHandle: SavedStateHandle,
    editLeagueViewModel: EditLeagueViewModel,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val setupState by setupViewModel.setup.collectAsState()
    val competitionsState by setupViewModel.competitions.collectAsState()
    /*val competitions by setupViewModel.competitions.collectAsState()
    val availableCompetitions = remember(competitions) {
        competitions
            .filter { it.isSelected == true }
            .map { it.item }
    }*/

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

    // In SetupEditLeagueScreen.kt
    LaunchedEffect(competitionsState) {
        if (competitionsState.isNotEmpty()) {
            // Extract the .item from each SelectableItem
            val competitionsOnly = competitionsState.map { it.item }
            editLeagueViewModel.setAvailableCompetitions(competitionsOnly)
        }
    }

    val username: String = savedStateHandle.get<String>("username") ?: ""
    val setup by setupViewModel.setup.collectAsState()
    val ownerUserEmail: String = setup?.email ?: ""

    val selectedCompetition by editLeagueViewModel.selectedCompetition.collectAsState()
    val leagueHeader by editLeagueViewModel.leagueHeader.collectAsState()
    val leagueName by editLeagueViewModel.leagueName.collectAsState()
    val leaguecode by editLeagueViewModel.leaguecode.collectAsState()
    val inviteEmailInput by editLeagueViewModel.inviteEmailInput.collectAsState()
    val invitedEmails by editLeagueViewModel.invitedEmails.collectAsState()
    val saveResult by editLeagueViewModel.saveResult.collectAsState()
    val inviteStatus by editLeagueViewModel.saveResult.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(leagueid, owneruserid) {
        editLeagueViewModel.fetchLeagueCode(leagueid, owneruserid)
    }

    LaunchedEffect(key1 = leagueid, key2 = owneruserid) {
        editLeagueViewModel.onEvent(EditLeagueViewModel.LeagueEvent.GetLeagueDetails(
            leagueid,
            owneruserid)
        )
    }

    // LaunchedEffect for showing Toasts for invite status (e.g., "Email added")
    LaunchedEffect(inviteStatus) {
        inviteStatus.let { message ->
            //Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            editLeagueViewModel.onInviteStatusConsumed()
        }
    }

    LaunchedEffect(saveResult) {
        // Use a local variable for the result inside the 'when' for cleaner code
        when (val result = saveResult) {
            is EditLeagueViewModel.EditLeagueResult.Success -> {
                // --- FIX IS HERE ---
                // Access the 'message' property of your 'Success' data class
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()

                // Now you can navigate back
                navController.popBackStack()
            }
            is EditLeagueViewModel.EditLeagueResult.Error -> {
                // The same fix applies here for the Error class
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                editLeagueViewModel.onResultConsumed() // Reset the error state
            }
            else -> {
                // Do nothing for Idle or Loading states
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Standard height for a top app bar
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
                    text = stringResource(id = R.string.edit_league),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        contentWindowInsets = WindowInsets.ime,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editLeagueViewModel.saveLeagueDetails()
                },
                containerColor = if (
                    saveResult is EditLeagueViewModel.EditLeagueResult.Loading
                ) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                if (saveResult is EditLeagueViewModel.EditLeagueResult.Loading) {
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
                //.consumeWindowInsets(contentPadding)
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                OutlinedTextField(
                    // Connect to the ViewModel's state and event
                    value = leagueName,
                    onValueChange = editLeagueViewModel::onLeagueNameChanged,
                    label = { Text(stringResource(id = R.string.league_name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.league_code),
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = leaguecode,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.league_share),
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Box( contentAlignment = Alignment.Center ) {
                    AdBanner(
                        modifier = Modifier.fillMaxWidth(),
                        isMediumRectangle = true
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(14.dp))
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
                                    color = MaterialTheme.colorScheme.primary),
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
                            val isRegionExpanded = expandedRegions["${sportType}_$regionName"] ?: false
                            Surface(
                                onClick = { expandedRegions["${sportType}_$regionName"] = !isRegionExpanded },
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
                                            color = MaterialTheme.colorScheme.primary),
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
                            items(competitionList, key = { "comp_${it.item.competitionid}" }) { competition ->
                                SelectableRowWithCheckboxes(
                                    modifier = Modifier.animateItem(),
                                    item = competition.item,
                                    name = competition.item.name,
                                    isSelected = (competition.item.id == selectedCompetition?.id),
                                    onCheckedChange = {
                                        editLeagueViewModel.onCompetitionSelected(competition.item)
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

            /*
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
                val coroutineScope = rememberCoroutineScope()
                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inviteEmailInput,
                        onValueChange = editLeagueViewModel::onInviteEmailChanged,
                        label = { Text(stringResource(id = R.string.users_email)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        // This pulls the field up above the keyboard toolbar
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }
                    )
                    // Button to ADD the email to the list
                    Button(
                        onClick = editLeagueViewModel::onEmailAdded,
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
                            .weight(1f))
                    IconButton(
                        onClick = {
                            editLeagueViewModel.onEmailRemoved(email)
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Email")
                    }
                }
            }*/

            // Add padding at the bottom of the list to ensure the FAB doesn't hide content
            /*item {
                Spacer(modifier = Modifier.height(80.dp))
            }*/
        }
    }
}