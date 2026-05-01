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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.theme.dropdown_background
import cloud.scoreprof.app.ui.theme.sub_dropdown_background
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompetitionsScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    passedUserId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val setupState by setupViewModel.setup.collectAsState()
    val userid = remember(setupState?.userid, passedUserId) {
        setupState?.userid ?: passedUserId?.let { UUID.fromString(it) }
    }
    val competitionsState by setupViewModel.competitions.collectAsState()

    LaunchedEffect(userid) {
        println("TEST: competitionsState: $competitionsState")
        println("TEST: : setupState: $setupState")
    }

    val groupedData = remember(competitionsState) {
        val collator = java.text.Collator.getInstance()

        competitionsState
            .filter { it.isSelected }
            .groupBy { it.item.sport_type ?: "Other" }
            .mapValues { sportEntry ->
                // 1. Group by Country/Region
                val byRegion = sportEntry.value.groupBy { it.item.region ?: "International" }

                // 2. Sort the Region Names (Keys) using the locale-aware Collator
                // 3. Sort the Competitions (Values) within each region by country_ranking
                byRegion.toSortedMap(collator::compare).mapValues { regionEntry ->
                    regionEntry.value.sortedBy { competitionSelection ->
                        competitionSelection.item.country_ranking ?: Int.MAX_VALUE
                    }
                }
            }.toSortedMap()
    }

    val expandedSports = remember { mutableStateMapOf<String, Boolean>() }
    val expandedRegions = remember { mutableStateMapOf<String, Boolean>() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Trigger a silent refresh from the server
                if (userid != null) {
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
                    .height(56.dp) // Standard height for a top app bar
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
                    text = stringResource(id = R.string.matches),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f) // Ensures title takes up available space
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth(),
            contentPadding = innerPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

                        // --- LEVEL 3: COMPETITION BUTTONS ---
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
                                        .animateItem()
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
                        isMediumRectangle = true
                    )
                }
            }
        }
    }
}