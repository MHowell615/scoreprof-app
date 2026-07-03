package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.Color
import scoreprof_resources.Res
import scoreprof_resources.leagues
import scoreprof_resources.public_leagues
import scoreprof_resources.private_leagues
import scoreprof_resources.delete_league
import scoreprof_resources.delete_league_confirmation
import scoreprof_resources.enter_code_hint
import scoreprof_resources.join_btn
import scoreprof_resources.join_league_title
import scoreprof_resources.delete
import scoreprof_resources.cancel
import scoreprof_resources.edit_league
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.utils.SelectableRowWithCheckboxes
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import cloud.scoreprof.app.getStringComparator

@Composable
fun SetupLeaguesScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val leagues by setupViewModel.setupLeagues.collectAsState()
    val publicOwnerId = "00000000-0000-0000-0000-111111111111"
    val comparator = remember { getStringComparator() }
    
    val groupedActiveLeagues = remember(leagues) {
        val filtered = leagues
            .filter { it.item.state?.uppercase() != "DELETED" }
            .distinctBy { "${it.item.leagueid}_${it.item.owneruserid}" }
            .sortedWith { a, b -> comparator.compare(a.item.name, b.item.name) }

        val grouped = LinkedHashMap<String, List<cloud.scoreprof.app.ui.view_models.SelectableItem<Leagues>>>()
        
        val private = filtered.filter { 
            it.item.owneruserid != publicOwnerId && !it.item.leagueid.equals("All", ignoreCase = true) 
        }
        if (private.isNotEmpty()) grouped["Private"] = private

        val public = filtered.filter { 
            it.item.owneruserid == publicOwnerId || it.item.leagueid.equals("All", ignoreCase = true) 
        }
        if (public.isNotEmpty()) grouped["Public"] = public
        
        grouped
    }
    
    var expandedLeagueItem by remember { mutableStateOf<Leagues?>(null) }
    var leagueToDelete by remember { mutableStateOf<Leagues?>(null) }
    val setup by setupViewModel.setup.collectAsState()
    val userid = setup?.userid
    val username = setup?.name ?: ""
    val email = setup?.email ?: ""
    var joinCode by remember { mutableStateOf("")}

    leagueToDelete?.let { selectedLeagues ->
        AlertDialog(
            onDismissRequest = { leagueToDelete = null },
            title = { Text(stringResource(Res.string.delete_league)) },
            text = { Text(stringResource(Res.string.delete_league_confirmation) + " '${selectedLeagues.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        setupViewModel.softDeleteLeague(selectedLeagues)
                        leagueToDelete = null
                    }
                ) { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { leagueToDelete = null }) { Text(stringResource(Res.string.cancel), color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                    text = stringResource(Res.string.leagues),
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
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.join_league_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { if (it.length <= 10) joinCode = it.uppercase() },
                                label = { Text(stringResource(Res.string.enter_code_hint)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { setupViewModel.requestJoinLeague(joinCode) },
                                enabled = joinCode.length >= 5
                            ) {
                                Text(stringResource(Res.string.join_btn))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(Res.string.leagues),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            item {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = button_background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        if (userid != null) {
                            navController.navigate("create_new_league/${userid}/${username}/{$email}")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Text(
                        text = "+",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = TextStyle(
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            groupedActiveLeagues.forEach { (category, leagueList) ->
                item {
                    Text(
                        text = if (category == "Public") stringResource(Res.string.public_leagues) else stringResource(Res.string.private_leagues),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(
                    leagueList,
                    key = { "${it.item.leagueid}_${it.item.owneruserid}" }
                ) { selectableLeague ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f)) {
                            SelectableRowWithCheckboxes(
                                item = selectableLeague.item,
                                name = selectableLeague.item.name,
                                isSelected = selectableLeague.isSelected,
                                onCheckedChange = { isSelected ->
                                    setupViewModel.onLeagueSelected(selectableLeague.item, isSelected)
                                }
                            )
                        }
                        if (selectableLeague.item.owneruserid == userid) {
                            Box {
                                IconButton(onClick = {
                                    expandedLeagueItem = selectableLeague.item
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                DropdownMenu(
                                    expanded = expandedLeagueItem == selectableLeague.item,
                                    onDismissRequest = { expandedLeagueItem = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.edit_league)) },
                                        onClick = {
                                            expandedLeagueItem = null
                                            val leagueid = selectableLeague.item.leagueid
                                            val owneruserid = selectableLeague.item.owneruserid
                                            if (userid != null) {
                                                navController.navigate(
                                                    "setup_edit_league_screen/${leagueid}/${owneruserid}/${userid}"
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.delete_league)) },
                                        onClick = {
                                            expandedLeagueItem = null
                                            leagueToDelete = selectableLeague.item
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
