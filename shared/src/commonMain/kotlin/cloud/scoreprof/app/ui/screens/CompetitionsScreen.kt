package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import scoreprof_resources.Res
import scoreprof_resources.matches
import scoreprof_resources.show_forthcoming_only
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.theme.sub_dropdown_background
import cloud.scoreprof.app.getStringComparator
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompetitionsScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    passedUserId: String? = null,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val setupState by setupViewModel.setup.collectAsState()
    val userid = remember(setupState?.userid, passedUserId) {
        setupState?.userid ?: passedUserId ?: ""
    }
    val isGuest = userid == "00000000-0000-0000-0000-000000000000"
    val competitionsState by setupViewModel.competitions.collectAsState()
    val showOnlyUpcoming by setupViewModel.showOnlyUpcoming.collectAsState()
    val comparator = remember { getStringComparator() }

    val displayedCompetitions = remember(competitionsState, showOnlyUpcoming) {
        if (showOnlyUpcoming) {
            competitionsState.filter { it.item.has_upcoming == true }
        } else {
            competitionsState
        }
    }

    val groupedData = remember(displayedCompetitions, isGuest) {
        displayedCompetitions
            .filter { it.isSelected || isGuest } // Show all for guest
            .groupBy { it.item.sport_type ?: "Other" }
            .mapValues { sportEntry ->
                val byRegion = sportEntry.value.groupBy { it.item.region ?: "International" }
                byRegion.toList()
                    .sortedWith { a, b -> comparator.compare(a.first, b.first) }
                    .toMap()
                    .mapValues { regionEntry ->
                        regionEntry.value.sortedBy { it.item.country_ranking ?: Int.MAX_VALUE }
                    }
            }
            .toList()
            .sortedWith { a, b -> comparator.compare(a.first, b.first) }
            .toMap()
    }

    val expandedSports = remember { mutableStateMapOf<String, Boolean>() }
    val expandedRegions = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(userid) {
        if (userid.isNotEmpty()) {
            setupViewModel.loadInitialDataForUser(userid)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (userid.isNotEmpty()) {
                    setupViewModel.refreshData()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
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
                    text = stringResource(Res.string.matches),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .padding(innerPadding)
                .padding(horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showOnlyUpcoming,
                        onCheckedChange = { setupViewModel.toggleUpcomingFilter(it) }
                    )
                    Text(
                        text = stringResource(Res.string.show_forthcoming_only),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            }
            for ((sportType, regions) in groupedData) {
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
                                text = sportType,
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
                    for ((regionName, competitionList) in regions) {
                        item(key = "region_${sportType}_$regionName") {
                            val isRegionExpanded = expandedRegions["${sportType}_$regionName"] ?: false
                            Surface(
                                onClick = { expandedRegions["${sportType}_$regionName"] = !isRegionExpanded },
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

                        if (expandedRegions["${sportType}_$regionName"] == true) {
                            items(
                                competitionList,
                                key = { "comp_${sportType}_${regionName}_${it.item.competitionid}" }
                            ) { selection ->
                                Button(
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = button_background,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = {
                                        navController.navigate("match_screen/${selection.item.competitionid}/$userid")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth().padding(start = 12.dp)
                                ) {
                                    Text(
                                        text = selection.item.name,
                                        style = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
