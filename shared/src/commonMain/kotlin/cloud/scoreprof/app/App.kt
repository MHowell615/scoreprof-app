package cloud.scoreprof.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.ui.screens.*
import cloud.scoreprof.app.ui.view_models.*
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun App() {
    val platform: Platform = koinInject()
    val setupViewModel: ListSetupViewModel = koinViewModel()
    val setupState by setupViewModel.setup.collectAsState()
    
    val languageCode = setupState?.preferred_language ?: platform.language
    val isRtl = languageCode == "ar"
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    KoinContext {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(setupViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(setupViewModel: ListSetupViewModel) {
    val versionViewModel: VersionViewModel = koinViewModel()
    val tokenManager: TokenManager = koinInject()
    val navController = rememberNavController()
    val isUpdateRequired by versionViewModel.isUpdateRequired.collectAsState()
    val updateUrl by versionViewModel.updateUrl.collectAsState()

    if (isUpdateRequired) {
        ForcedUpdateScreen(updateUrl)
    } else {
        val isLoggedIn = tokenManager.hasToken() && tokenManager.getUserId() != null && tokenManager.getEmail() != null
        val isGuest = tokenManager.getUserId() == "00000000-0000-0000-0000-000000000000"
        
        val startDestination = if (isLoggedIn || isGuest) {
            val userid = tokenManager.getUserId()!!
            val email = tokenManager.getEmail() ?: "guest@scoreprof.cloud"
            val encodedEmail = email.replace("@", "%40")
            "main_graph/$userid/$encodedEmail"
        } else {
            "login"
        }

        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") {
                LoginScreen(onLoginSuccess = { userid, email ->
                    val targetUserId = if (userid == "guest") "00000000-0000-0000-0000-000000000000" else userid
                    val targetEmail = if (userid == "guest") "guest@scoreprof.cloud" else email
                    
                    tokenManager.saveUserId(targetUserId)
                    tokenManager.saveEmail(targetEmail)
                    
                    val encodedEmail = targetEmail.replace("@", "%40")
                    navController.navigate("main_graph/$targetUserId/$encodedEmail") {
                        popUpTo("login") { inclusive = true }
                    }
                }, navController = navController)
            }

            navigation(
                startDestination = "home",
                route = "main_graph/{userid}/{email}",
                arguments = listOf(
                    navArgument("userid") { type = NavType.StringType },
                    navArgument("email") { type = NavType.StringType }
                )
            ) {
                composable("home") { navBackStackEntry ->
                    val parentEntry = remember(navBackStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val useridString = parentEntry.savedStateHandle.get<String>("userid")
                    val email = parentEntry.savedStateHandle.get<String>("email")
                    val tokenManager: TokenManager = koinInject()

                    LaunchedEffect(Unit) {
                        setupViewModel.navigationEvents.collect { event ->
                            if (event is ListSetupViewModel.NavigationEvent.ToLogin) {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }

                    HomeScreen(
                        setupViewModel = setupViewModel,
                        navController = navController,
                        passedUserId = useridString,
                        passedEmail = email,
                        tokenManager = tokenManager
                    )
                }

                composable(
                    "competitions_screen/{userid}",
                    arguments = listOf(navArgument("userid") { type = NavType.StringType })
                ) { navBackStackEntry ->
                    val useridString = navBackStackEntry.savedStateHandle.get<String>("userid")
                    
                    CompetitionsScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        passedUserId = useridString
                    )
                }

                composable(
                    "notifications_screen/{notificationid}",
                    arguments = listOf(navArgument("notificationid") { type = NavType.IntType })
                ) { backStackEntry ->
                    val notificationid = backStackEntry.savedStateHandle.get<Int>("notificationid") ?: 0
                    val notificationViewModel: NotificationViewModel = koinViewModel()

                    NotificationScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        notificationViewModel = notificationViewModel,
                        notificationId = notificationid
                    )
                }

                composable(
                    "setup_screen/{userid}",
                    arguments = listOf(navArgument("userid") { type = NavType.StringType })
                ) {
                    SetupScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("languages_screen") {
                    LanguagesScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("setup_competitions_screen/{userid}") {
                    SetupCompetitionsScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("setup_privacy_screen/{userid}") {
                    SetupPrivacyScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("setup_leagues_screen/{userid}") {
                    SetupLeaguesScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable(
                    "setup_edit_league_screen/{leagueid}/{owneruserid}/{userid}",
                    arguments = listOf(
                        navArgument("leagueid") { type = NavType.StringType },
                        navArgument("owneruserid") { type = NavType.StringType },
                        navArgument("userid") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val leagueid = backStackEntry.savedStateHandle.get<String>("leagueid") ?: ""
                    val owneruseridString = backStackEntry.savedStateHandle.get<String>("owneruserid") ?: ""
                    val useridString = backStackEntry.savedStateHandle.get<String>("userid") ?: ""

                    val editLeagueViewModel: EditLeagueViewModel = koinViewModel()

                    LaunchedEffect(leagueid, owneruseridString) {
                        editLeagueViewModel.getLeagueDetails(leagueid, owneruseridString)
                    }

                    SetupEditLeagueScreen(
                        navController = navController,
                        leagueid = leagueid,
                        owneruserid = owneruseridString,
                        userid = useridString,
                        savedStateHandle = backStackEntry.savedStateHandle,
                        editLeagueViewModel = editLeagueViewModel,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable(
                    route = "match_screen/{competitionId}/{userid}",
                    arguments = listOf(
                        navArgument("competitionId") { type = NavType.StringType },
                        navArgument("userid") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val competitionId = backStackEntry.savedStateHandle.get<String>("competitionId")
                    val userid = backStackEntry.savedStateHandle.get<String>("userid")
                    val viewModel: ListMatchesViewModel = koinViewModel()
                    val competitions by setupViewModel.competitions.collectAsState()
                    
                    if (userid.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val competitionName = competitions.find { it.item.competitionid == competitionId }?.item?.name ?: "Competition"
                        if (competitionId != null) {
                            LaunchedEffect(key1 = competitionId) {
                                viewModel.onEvent(ListMatchesViewModel.MatchEvent.LoadMatchesForCompetition(competitionId))
                            }
                            ListMatchesScreen(
                                competitionid = competitionId,
                                competitionName = competitionName,
                                navController = navController,
                                setupViewModel = setupViewModel,
                                matchesViewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                composable(
                    route = "league_screen/{leagueid}/{owneruserid}/{leaguename}",
                    arguments = listOf(
                        navArgument("leagueid") { type = NavType.StringType },
                        navArgument("owneruserid") { type = NavType.StringType },
                        navArgument("leaguename") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val leagueid = backStackEntry.savedStateHandle.get<String>("leagueid") ?: ""
                    val owneruseridString = backStackEntry.savedStateHandle.get<String>("owneruserid") ?: ""
                    val leaguename = backStackEntry.savedStateHandle.get<String>("leaguename") ?: ""

                    val listLeagueViewModel: ListLeagueViewModel = koinViewModel()

                    LaunchedEffect(leagueid, owneruseridString) {
                        if (leagueid.isNotEmpty() && owneruseridString.isNotEmpty()) {
                            listLeagueViewModel.onEvent(ListLeagueViewModel.LeagueEvent.LoadLeagueTable(leagueid, owneruseridString))
                        }
                    }

                    ListLeagueScreen(
                        leagueid = leagueid,
                        owneruserid = owneruseridString,
                        leaguename = leaguename,
                        navController = navController,
                        leagueViewModel = listLeagueViewModel,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable(
                    "create_new_league/{userid}/{username}/{email}",
                    arguments = listOf(
                        navArgument("userid") { type = NavType.StringType },
                        navArgument("username") { type = NavType.StringType },
                        navArgument("email") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    CreateNewLeagueScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        savedStateHandle = backStackEntry.savedStateHandle
                    )
                }

                composable(
                    "leagues_screen/{userid}",
                    arguments = listOf(navArgument("userid") { type = NavType.StringType })
                ) {
                    val listLeaguesViewModel: ListLeaguesViewModel = koinViewModel()
                    LeaguesScreen(
                        navController = navController,
                        leaguesViewModel = listLeaguesViewModel,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("help_screen") {
                    val listHelpViewModel: ListHelpViewModel = koinViewModel()
                    HelpScreen(
                        navController = navController,
                        helpViewModel = listHelpViewModel,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("contact_screen") {
                    ContactScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("forgot_password") {
                    val loginViewModel: LoginViewModel = koinViewModel()
                    ForgotPasswordScreen(loginViewModel, navController)
                }
            }
        }
    }
}
