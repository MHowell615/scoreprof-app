package cloud.scoreprof.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cloud.scoreprof.app.R
import cloud.scoreprof.app.ui.components.AdBanner
import cloud.scoreprof.app.ui.theme.button_background
import cloud.scoreprof.app.ui.view_models.ListLeaguesViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel

@Composable
fun LeaguesScreen(
    navController: NavHostController,
    leaguesViewModel: ListLeaguesViewModel,
    setupViewModel: ListSetupViewModel,
    modifier: Modifier = Modifier
) {
    val leagues by leaguesViewModel.leagues.collectAsState()
    val setupState by setupViewModel.setup.collectAsState()

    println("TEST: leagues = $leagues")
    val filteredLeagues = leagues.filter {
        it.state?.uppercase() != "DELETED"
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
                        text = stringResource(id = R.string.leagues),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
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
            items(filteredLeagues) { league ->
                Row(
                    modifier = modifier
                        .background(color = button_background)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = button_background,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            val leagueid = league.leagueid
                            val owneruserid = league.owneruserid
                            val leaguename = league.name
                            navController.navigate("league_screen/${leagueid}/${owneruserid}/${leaguename}")
                        },
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = league.name,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = ("Forward")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (setupState?.is_ads_removed == false) {
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
                            isMediumRectangle = false,
                            showAds = true
                        )
                    }
                }
            }
         }
    }
}
