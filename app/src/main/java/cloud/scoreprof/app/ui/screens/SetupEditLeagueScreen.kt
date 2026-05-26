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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.theme.sub_dropdown_background
import cloud.scoreprof.app.ui.utils.SelectableRowWithCheckboxes
import cloud.scoreprof.app.ui.view_models.EditLeagueViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
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

    LaunchedEffect(competitionsState) {
        if (competitionsState.isNotEmpty()) {
            val competitionsOnly = competitionsState.map { it.item }
            editLeagueViewModel.setAvailableCompetitions(competitionsOnly)
        }
    }

    val selectedCompetition by editLeagueViewModel.selectedCompetition.collectAsState()
    val leagueName by editLeagueViewModel.leagueName.collectAsState()
    val leaguecode by editLeagueViewModel.leaguecode.collectAsState()
    val saveResult by editLeagueViewModel.saveResult.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(leagueid, owneruserid) {
        editLeagueViewModel.fetchLeagueCode(leagueid, owneruserid)
        editLeagueViewModel.onEvent(EditLeagueViewModel.LeagueEvent.GetLeagueDetails(leagueid, owneruserid))
    }

    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is EditLeagueViewModel.EditLeagueResult.Success -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            is EditLeagueViewModel.EditLeagueResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                editLeagueViewModel.onResultConsumed()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = stringResource(id = R.string.edit_league),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editLeagueViewModel.saveLeagueDetails() },
                containerColor = if (saveResult is EditLeagueViewModel.EditLeagueResult.Loading) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                if (saveResult is EditLeagueViewModel.EditLeagueResult.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                }
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                OutlinedTextField(
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(text = stringResource(id = R.string.league_code), style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = leaguecode, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                }
                Text(text = stringResource(id = R.string.league_share), style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.primary), modifier = Modifier.padding(vertical = 4.dp))
            }
            
            // Ad Banner with removal logic
            if (setupState?.is_ads_removed == false) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(contentAlignment = Alignment.Center) {
                        AdBanner(modifier = Modifier.fillMaxWidth(), isMediumRectangle = true, showAds = true)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(id = R.string.competitions),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            groupedData.forEach { (sportType, regions) ->
                item(key = "sport_$sportType") {
                    val isSportExpanded = expandedSports[sportType] ?: false
                    Surface(
                        onClick = { expandedSports[sportType] = !isSportExpanded },
                        color = dropdown_background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = sportType.uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                            Icon(imageVector = if (isSportExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (expandedSports[sportType] == true) {
                    regions.forEach { (regionName, competitionList) ->
                        item(key = "region_${sportType}_$regionName") {
                            val isRegionExpanded = expandedRegions["${sportType}_$regionName"] ?: false
                            Surface(
                                onClick = { expandedRegions["${sportType}_$regionName"] = !isRegionExpanded },
                                color = sub_dropdown_background,
                                contentColor = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = regionName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                                    Icon(imageVector = if (isRegionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        if (expandedRegions["${sportType}_$regionName"] == true) {
                            items(competitionList, key = { "comp_${it.item.competitionid}" }) { competition ->
                                SelectableRowWithCheckboxes(
                                    modifier = Modifier.animateItem(),
                                    item = competition.item,
                                    name = competition.item.name,
                                    isSelected = (competition.item.id == selectedCompetition?.id),
                                    onCheckedChange = { editLeagueViewModel.onCompetitionSelected(competition.item) }
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}
