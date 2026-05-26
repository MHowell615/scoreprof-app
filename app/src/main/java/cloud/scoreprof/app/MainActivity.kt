package cloud.scoreprof.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cloud.scoreprof.app.ui.screens.CreateNewLeagueScreen
import cloud.scoreprof.app.ui.screens.HomeScreen
import cloud.scoreprof.app.ui.screens.LeaguesScreen
import cloud.scoreprof.app.ui.screens.ListLeagueScreen
import cloud.scoreprof.app.ui.view_models.ListLeagueViewModel
import cloud.scoreprof.app.ui.view_models.ListLeaguesViewModel
import cloud.scoreprof.app.ui.screens.ListMatchesScreen
import cloud.scoreprof.app.ui.view_models.ListMatchesViewModel
import cloud.scoreprof.app.ui.view_models.ListSetupViewModel
import cloud.scoreprof.app.ui.screens.LoginScreen
import cloud.scoreprof.app.ui.screens.SetupScreen
import cloud.scoreprof.app.ui.screens.SetupEditLeagueScreen
import cloud.scoreprof.app.ui.theme.TestNavigationTheme
import kotlinx.serialization.Serializable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.navigation
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.ui.screens.ContactScreen
import cloud.scoreprof.app.ui.view_models.ListHelpViewModel
import cloud.scoreprof.app.ui.screens.HelpScreen
import cloud.scoreprof.app.ui.screens.LanguagesScreen
import cloud.scoreprof.app.ui.screens.SetupCompetitionsScreen
import cloud.scoreprof.app.ui.screens.SetupLeaguesScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.remember
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.Alignment
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import cloud.scoreprof.app.ui.view_models.EditLeagueViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.ui.screens.CompetitionsScreen
import cloud.scoreprof.app.ui.view_models.VersionViewModel
import cloud.scoreprof.app.ui.screens.ForcedUpdateScreen
import cloud.scoreprof.app.ui.screens.ForgotPasswordScreen
import cloud.scoreprof.app.ui.screens.NotificationScreen
import cloud.scoreprof.app.ui.screens.SetupPrivacyScreen
import cloud.scoreprof.app.ui.view_models.LoginViewModel
import cloud.scoreprof.app.ui.view_models.NotificationViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NavigationConfig(
    val version: Int,
    val startDestination: String,
    val pages: List<PageConfig>
)

@Serializable
data class PageConfig(
    val route: String,
    val type: String,
    val screenType: String? = null,
    val competitionid: String? = null,
    val competition: String? = null,
    val nextRoute: String? = null,
    val title: String? = null,
    val options: List<OptionConfig>? = null,
    val id: String? = null,
)

@Serializable
data class OptionConfig(
    val name: String? = null,
    val route: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dao: ScoreProfDao

    @Inject
    lateinit var tokenManager: TokenManager

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.e("AppUpdate", "Update flow failed! Result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Enable modern edge-to-edge behavior
        enableEdgeToEdge()

        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { loadAndShowError ->
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }
                }
            },
            { requestConsentError ->
                Log.w("MainActivity", "${requestConsentError.errorCode}: ${requestConsentError.message}")
                initializeMobileAdsSdk()
            }
        )

        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }

        runBlocking {
            dao.getSetup().firstOrNull()?.preferred_language?.let { langCode ->
                val locale = Locale.forLanguageTag(langCode)
                val appLocale = LocaleListCompat.create(locale)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        }

        setContent {
            TestNavigationTheme {
                // Let the Scaffold handle all system bar insets automatically
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(tokenManager = tokenManager)
                }
            }
        }
    }

    private val isMobileAdsInitializeCalled = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        MobileAds.initialize(this) { initializationStatus ->
            Log.d("MainActivity", "Ads SDK Initialized: $initializationStatus")
        }
    }
}

@Composable
fun AppNavigation(
    tokenManager: TokenManager
) {
    val versionViewModel: VersionViewModel = hiltViewModel()
    val navController = rememberNavController()
    val isUpdateRequired by versionViewModel.isUpdateRequired.collectAsState()
    val updateUrl by versionViewModel.updateUrl.collectAsState()

    if (isUpdateRequired) {
        ForcedUpdateScreen(updateUrl)
    } else {
        val startDestination = "login"
        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") {
                LoginScreen(onLoginSuccess = { userid, email ->
                    navController.navigate("main_graph/$userid/$email") {
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
                    val parentEntry = remember (navBackStackEntry) {
                        try {
                            navController.getBackStackEntry("main_graph/{userid}/{email}")
                        } catch (e: Exception) {
                            navBackStackEntry
                        }
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)

                    LaunchedEffect(Unit) {
                        setupViewModel.navigationEvents.collect { event ->
                            when (event) {
                                is ListSetupViewModel.NavigationEvent.ToLogin -> {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    val useridString = navBackStackEntry.arguments?.getString("userid")
                    val currentUserid = remember(useridString) {
                        useridString?.let { UUID.fromString(it) }
                    }
                    val email = navBackStackEntry.arguments?.getString("email")

                    LaunchedEffect(currentUserid) {
                        if (currentUserid != null) {
                            setupViewModel.loadInitialDataForUser(currentUserid)
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
                    val parentEntry = remember(navBackStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    val useridString = navBackStackEntry.arguments?.getString("userid")
                    
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
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    val notificationViewModel: NotificationViewModel = hiltViewModel()
                    val notificationid = backStackEntry.arguments?.getInt("notificationid") ?: 0

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
                ) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    SetupScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("languages_screen") { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    LanguagesScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("setup_competitions_screen/{userid}") { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    SetupCompetitionsScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("setup_privacy_screen/{userid}") { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    SetupPrivacyScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("setup_leagues_screen/{userid}") { navBackStackEntry ->
                    val parentEntry = remember(navBackStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
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
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    val editLeagueViewModel: EditLeagueViewModel = hiltViewModel()
                    val leagueid = backStackEntry.arguments?.getString("leagueid") ?: ""
                    val owneruseridString = backStackEntry.arguments!!.getString("owneruserid")
                    val useridString = backStackEntry.arguments!!.getString("userid")
                    val owneruserid = try { owneruseridString?.let { UUID.fromString(it) } } catch (e: Exception) { null }
                    val userid = try { useridString?.let { UUID.fromString(it) } } catch (e: Exception) { null }

                    if (owneruserid != null && userid != null) {
                        LaunchedEffect(leagueid, owneruserid) {
                            editLeagueViewModel.getLeagueDetails(leagueid, owneruserid)
                        }
                        SetupEditLeagueScreen(
                            navController = navController,
                            leagueid = leagueid,
                            owneruserid = owneruserid,
                            userid = userid,
                            savedStateHandle = backStackEntry.savedStateHandle,
                            editLeagueViewModel = editLeagueViewModel,
                            setupViewModel = setupViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Invalid User IDs")
                        }
                    }
                }

                composable(
                    route = "match_screen/{competitionId}/{userid}",
                    arguments = listOf(
                        navArgument("competitionId") { type = NavType.StringType },
                        navArgument("userid") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    val viewModel: ListMatchesViewModel = hiltViewModel()
                    val competitionId = backStackEntry.arguments!!.getString("competitionId")
                    val competitions by setupViewModel.competitions.collectAsState()
                    val userid = backStackEntry.arguments!!.getString("userid")
                    
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
                    val listLeagueViewModel: ListLeagueViewModel = hiltViewModel()
                    val listSetupViewModel: ListSetupViewModel = hiltViewModel()
                    val leagueid = backStackEntry.arguments?.getString("leagueid") ?: ""
                    val owneruseridString = backStackEntry.arguments?.getString("owneruserid")
                    val owneruserid = try { UUID.fromString(owneruseridString) } catch (e: Exception) { null }
                    val leaguename = backStackEntry.arguments?.getString("leaguename") ?: ""

                    LaunchedEffect(leagueid, owneruserid) {
                        if (leagueid.isNotEmpty() && owneruserid != null) {
                            listLeagueViewModel.onEvent(ListLeagueViewModel.LeagueEvent.LoadLeagueTable(leagueid, owneruserid))
                        }
                    }

                    if (leagueid != null && owneruserid != null && leaguename != null) {
                        ListLeagueScreen(
                            leagueid = leagueid,
                            owneruserid = owneruserid,
                            leaguename = leaguename,
                            navController = navController,
                            leagueViewModel = listLeagueViewModel,
                            setupViewModel = listSetupViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                composable(
                    "create_new_league/{userid}/{username}/{email}",
                    arguments = listOf(
                        navArgument("userid") { type = NavType.StringType },
                        navArgument("username") { type = NavType.StringType },
                        navArgument("email") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
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
                    val listLeaguesViewModel: ListLeaguesViewModel = hiltViewModel()
                    val parentEntry = remember (it) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    LeaguesScreen(
                        navController = navController,
                        leaguesViewModel = listLeaguesViewModel,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("help_screen") {
                    val listHelpViewModel: ListHelpViewModel = hiltViewModel()
                    val listSetupViewModel: ListSetupViewModel = hiltViewModel()
                    HelpScreen(
                        navController = navController,
                        helpViewModel = listHelpViewModel,
                        setupViewModel = listSetupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("contact_screen") { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    ContactScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("forgot_password") {
                    val loginViewModel = hiltViewModel<LoginViewModel>()
                    ForgotPasswordScreen(loginViewModel, navController)
                }
            }
        }
    }
}

@Composable
fun NavBackStackEntry.sharedViewModel(navController: NavHostController): ListSetupViewModel {
    val navGraphRoute = "main_graph/{userid}/{email}"
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return hiltViewModel(parentEntry)
}
