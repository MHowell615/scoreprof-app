package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.utils.SelectableRowWithCheckboxes
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateMapOf
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.theme.sub_dropdown_background

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetupCompetitionsScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val competitions by setupViewModel.competitions.collectAsState()
    val setupState by setupViewModel.setup.collectAsState()
    val userid = setupViewModel.userid

    val groupedAllCompetitions = remember(competitions) {
        competitions
            .groupBy { it.item.sport_type ?: "Other" }
            .mapValues { sportEntry ->
                sportEntry.value.groupBy { it.item.region ?: "International" }
                    .toSortedMap()
            }.toSortedMap()
    }

    val expandedSports = remember { mutableStateMapOf<String, Boolean>() }
    val expandedRegions = remember { mutableStateMapOf<String, Boolean>() }

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
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = stringResource(id = R.string.competitions),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 4.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
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
            groupedAllCompetitions.forEach { (sportType, regions) ->
                item(key = "sport_$sportType") {
                    val isSportExpanded = expandedSports[sportType] ?: false
                    Surface(
                        onClick = { expandedSports[sportType] = !isSportExpanded },
                        color = dropdown_background,
                        contentColor = MaterialTheme.colorScheme.primary,
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
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
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

                        if (expandedRegions["${sportType}_$regionName"] == true) {
                            items(competitionList, key = { "setup_${it.item.competitionid}" }) { selectableCompetition ->
                                SelectableRowWithCheckboxes(
                                    modifier = Modifier.animateItem(),
                                    item = selectableCompetition.item,
                                    name = selectableCompetition.item.name,
                                    isSelected = selectableCompetition.isSelected,
                                    onCheckedChange = { isSelected ->
                                        setupViewModel.onCompetitionSelected(selectableCompetition.item, isSelected)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(contentAlignment = Alignment.Center) {
                    AdBanner(
                        modifier = Modifier.fillMaxWidth(),
                        isMediumRectangle = true,
                        showAds = setupState?.is_ads_removed == false
                    )
                }
            }
        }
    }
}
