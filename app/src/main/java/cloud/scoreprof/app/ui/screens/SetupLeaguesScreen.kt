package cloud.scoreprof.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.utils.SelectableRowWithCheckboxes
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cloud.scoreprof.app.domain.model.Leagues
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.Firebase

@Composable
fun SetupLeaguesScreen(
    navController: NavHostController,
    setupViewModel: ListSetupViewModel,
    //savedStateHandle: SavedStateHandle,
    modifier: Modifier = Modifier
) {
    val leagues by setupViewModel.setupLeagues.collectAsState()
    val activeLeagues = remember(leagues) {
        leagues
            .filter { it.item.state?.uppercase() != "DELETED" }
            .distinctBy { "${it.item.leagueid}_${it.item.owneruserid}" }
    }
    var expandedLeagueItem by remember { mutableStateOf<Leagues?>(null) }
    val competitions by setupViewModel.competitions.collectAsState()
    var leagueToDelete by remember { mutableStateOf<Leagues?>(null) }
    val setup by setupViewModel.setup.collectAsState()
    val userid = setup?.userid
    val username = setup?.name ?: ""
    val email = setup?.email ?: ""

    // Log anonymous context to Crashlytics to help debug if this screen fails
    LaunchedEffect(userid) {
        Firebase.crashlytics.setCustomKey("screen", "SetupLeagues")
        Firebase.crashlytics.setCustomKey("has_userid", userid != null)
    }

    // Confirmation Dialog
    leagueToDelete?.let { selectedLeagues ->
        AlertDialog(
            onDismissRequest = { leagueToDelete = null },
            title = { Text(stringResource(id = R.string.delete_league)) },
            text = { Text(stringResource(R.string.delete_league_confirmation) + " '${selectedLeagues.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        setupViewModel.softDeleteLeague(selectedLeagues)
                        leagueToDelete = null
                    }
                ) { Text(stringResource(id = R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { leagueToDelete = null }) { Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.primary) }
            }
        )
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
                    text = stringResource(id = R.string.leagues),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f) // Ensures title takes up available space
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.leagues),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(
                activeLeagues,
                key = { "${it.item.leagueid}_${it.item.owneruserid}" }
            ) { selectableLeague ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. The main selectable area (Checkboxes + Name)
                            // We use weight(1f) to push the action icons to the right
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
                                    imageVector = Icons.Default.MoreVert, // You may need to import Icons.Default.MoreVert
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            DropdownMenu(
                                expanded = expandedLeagueItem == selectableLeague.item,
                                onDismissRequest = { expandedLeagueItem = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.edit_league)) },
                                    onClick = {
                                        expandedLeagueItem = null
                                        val leagueid = Uri.encode(selectableLeague.item.leagueid)
                                        val owneruserid = selectableLeague.item.owneruserid.toString()
                                        println("(SetupLeaguesScreen) Deleting league: $leagueToDelete")
                                        println("(SetupLeaguesScreen) userid: $userid")
                                        if (userid != null) {
                                            val userid = Uri.encode(userid.toString())
                                            println("TEST: (SetupLeaguesScreen) owneruserid=${owneruserid}")
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
                                    text = { Text(stringResource(id = R.string.delete_league)) },
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
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = button_background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {

                        //println("TEST: userid=$userid")
                        if (userid != null) {
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                key = "competitions",
                                value = ArrayList(competitions.map { it.item })
                            )
                            navController.navigate("create_new_league/${userid}/${username}/{$email}")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Text(
                        text = "+",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}