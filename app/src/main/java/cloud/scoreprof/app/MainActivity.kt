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

        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher, // Use the launcher instead of 'this' and a request code
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
                    // Consent has been gathered or isn't required
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }
                }
            },
            { requestConsentError ->
                // Consent gathering failed
                Log.w("MainActivity", "${requestConsentError.errorCode}: ${requestConsentError.message}")
                // Even if consent fails, we try to initialize for non-personalized ads
                initializeMobileAdsSdk()
            }
        )

        // Check if consent is already available on startup
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }

        runBlocking {
            dao.getSetup().firstOrNull()?.preferred_language?.let { langCode ->
                // Create a single Locale object from our two-letter code (e.g., "fr").
                val locale = Locale.forLanguageTag(langCode)

                // Create a LocaleListCompat containing ONLY our desired locale.
                val appLocale = LocaleListCompat.create(locale)

                // Set this as the app's locale list. This will override the system's [en-FR].
                AppCompatDelegate.setApplicationLocales(appLocale)

                Log.d("MainActivity_Locale", "Initial locale set synchronously to: $langCode")
            }
        }

        enableEdgeToEdge()
        setContent {
            //val setupViewModel: ListSetupViewModel = hiltViewModel()

            TestNavigationTheme {
                // Scaffold ensures your UI doesn't overlap with system bars
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavigation(
                            //setupViewModel = setupViewModel
                            tokenManager = tokenManager
                        )
                    }
                }
            }
        }
    }

    private val isMobileAdsInitializeCalled = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) { initializationStatus ->
            Log.d("MainActivity", "Ads SDK Initialized: $initializationStatus")
        }
    }
}

@Composable
fun AppNavigation(
    tokenManager: TokenManager
) {
    // This will no longer crash because we made the ID optional in the VM
    val versionViewModel: VersionViewModel = hiltViewModel()

    val navController = rememberNavController()

    val isUpdateRequired by versionViewModel.isUpdateRequired.collectAsState()
    val updateUrl by versionViewModel.updateUrl.collectAsState()

    if (isUpdateRequired) {
        ForcedUpdateScreen(updateUrl)
    } else {

        val startDestination = "login" //if (tokenManager.hasToken()) "main_graph_root" else "login"

        NavHost(navController = navController, startDestination = startDestination) {

            // --- LOGIN ---
            composable("login") {
                LoginScreen(onLoginSuccess = { userid, email ->
                    // Navigate and pass the real UUID to the graph
                    navController.navigate("main_graph/$userid/$email") {
                        popUpTo("login") { inclusive = true }
                    }
                }, navController = navController)
            }

            // --- MAIN APP ---
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
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)

                    LaunchedEffect(Unit) {
                        setupViewModel.navigationEvents.collect { event ->
                            when (event) {
                                is ListSetupViewModel.NavigationEvent.ToLogin -> {
                                    Log.d(
                                        "Navigation",
                                        "Session expired event received. Navigating to Login."
                                    )
                                    navController.navigate("login") {
                                        // Clear the backstack so the user can't "back" into the home screen
                                        popUpTo(0) { inclusive = true }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }

                    val useridString = navBackStackEntry.arguments?.getString("userid")
                    // Convert String back to UUID
                    val currentUserid = remember(useridString) {
                        useridString?.let { UUID.fromString(it) }
                    }
                    val email = navBackStackEntry.arguments?.getString("email")

                    // 2. ONLY trigger data loading when we have a valid ID from the route
                    LaunchedEffect(currentUserid) {
                        if (currentUserid != null) {
                            // This triggers the fetch that gets you past the loading screen
                            setupViewModel.loadInitialDataForUser(currentUserid)
                        }
                    }

                    val currentSetup by setupViewModel.setup.collectAsState()
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
                    arguments = listOf(
                        navArgument("userid") { type = NavType.StringType }
                    )
                ) { navBackStackEntry ->
                    val parentEntry = remember(navBackStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    val useridString = navBackStackEntry.arguments?.getString("userid")
                    // Convert String back to UUID
                    val currentUserid = remember(useridString) {
                        useridString?.let { UUID.fromString(it) }
                    }
                    val email = navBackStackEntry.arguments?.getString("email")

                    CompetitionsScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        passedUserId = useridString
                    )
                }

                composable(
                    "notifications_screen/{notificationid}",
                    arguments = listOf(
                        navArgument("notificationid") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    val notificationViewModel: NotificationViewModel = hiltViewModel()

                    val notificationid = backStackEntry.arguments?.getInt("notificationid") ?: 0
                    //val email = backStackEntry.arguments?.getString("email") ?: ""

                    val useridString = backStackEntry.arguments?.getString("userid")
                    val userid = try {
                        UUID.fromString(useridString)
                    } catch (e: Exception) {
                        null
                    }
                    NotificationScreen(
                        navController = navController,
                        setupViewModel = setupViewModel,
                        notificationViewModel = notificationViewModel,
                        notificationId = notificationid
                    )
                }

                composable(
                    "setup_screen/{userid}",
                    arguments = listOf(
                        navArgument("userid") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("main_graph/{userid}/{email}")
                    }
                    val setupViewModel: ListSetupViewModel = hiltViewModel(parentEntry)
                    val setupState by setupViewModel.setup.collectAsState()
                    val useridString = backStackEntry.arguments?.getString("userid")
                    val userid = try {
                        UUID.fromString(useridString)
                    } catch (e: Exception) {
                        null
                    }
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
                    val setupState by setupViewModel.setup.collectAsState()
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
                    val setupState by setupViewModel.setup.collectAsState()
                    val useridString = backStackEntry.arguments?.getString("userid")
                    val userid = try {
                        UUID.fromString(useridString)
                    } catch (e: Exception) {
                        null
                    }

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
                    val setupState by setupViewModel.setup.collectAsState()
                    val useridString = backStackEntry.arguments?.getString("userid")
                    val userid = try {
                        UUID.fromString(useridString)
                    } catch (e: Exception) {
                        null
                    }

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
                    val setupState by setupViewModel.setup.collectAsState()
                    val useridString = navBackStackEntry.arguments?.getString("userid")
                    val userid = try {
                        UUID.fromString(useridString)
                    } catch (e: Exception) {
                        null
                    }

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
                    val setupState by setupViewModel.setup.collectAsState()
                    val editLeagueViewModel: EditLeagueViewModel = hiltViewModel()
                    val leagueid = backStackEntry.arguments?.getString("leagueid") ?: ""

                    val owneruseridString = backStackEntry.arguments!!.getString("owneruserid")
                    val useridString = backStackEntry.arguments!!.getString("userid")
                    val owneruserid = try {
                        owneruseridString?.let { UUID.fromString(it) }
                    } catch (e: Exception) {
                        null
                    }
                    val userid = try {
                        useridString?.let { UUID.fromString(it) }
                    } catch (e: Exception) {
                        null
                    }

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
                        // Optional: Show an error or redirect if the UUIDs are invalid
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
                    val setupState by setupViewModel.setup.collectAsState()
                    val viewModel: ListMatchesViewModel = hiltViewModel()
                    val competitionId = backStackEntry.arguments!!.getString("competitionId")
                    val competitions by setupViewModel.competitions.collectAsState()
                    val userid = backStackEntry.arguments!!.getString("userid")
                    if (userid!!.isEmpty()) {
                        // If the ID is missing from the route, show a loader or error
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val competitionName = competitions
                            .find { it.item.competitionid == competitionId }?.item?.name
                            ?: "Competition"
                        if (competitionId != null) {
                            LaunchedEffect(key1 = competitionId) {
                                viewModel.onEvent(
                                    ListMatchesViewModel.MatchEvent.LoadMatchesForCompetition(
                                        competitionId
                                    )
                                )
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
                    route = "league_screen/{leagueid}/{owneruserid}",
                    arguments = listOf(
                        navArgument("leagueid") { type = NavType.StringType },
                        navArgument("owneruserid") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val listLeagueViewModel: ListLeagueViewModel = hiltViewModel()
                    val leagueid = backStackEntry.arguments?.getString("leagueid") ?: ""
                    val owneruseridString = backStackEntry.arguments?.getString("owneruserid")
                    val owneruserid = try {
                        UUID.fromString(owneruseridString)
                    } catch (e: Exception) {
                        null
                    }

                    LaunchedEffect(leagueid, owneruserid) {
                        if (leagueid.isNotEmpty() && owneruserid != null) {
                            listLeagueViewModel.onEvent(
                                ListLeagueViewModel.LeagueEvent.LoadLeagueTable(
                                    leagueid = leagueid,
                                    owneruserid = owneruserid
                                )
                            )
                        }
                    }

                    if (leagueid != null && owneruserid != null) {
                        ListLeagueScreen(
                            leagueid = leagueid,
                            owneruserid = owneruserid,
                            leaguename = leagueid,
                            navController = navController,
                            leagueViewModel = listLeagueViewModel,
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
                    arguments = listOf(
                        navArgument("userid") { type = NavType.StringType }
                    )
                ) {
                    val listLeaguesViewModel: ListLeaguesViewModel = hiltViewModel()
                    LeaguesScreen(
                        navController = navController,
                        //savedStateHandle = it.savedStateHandle,
                        leaguesViewModel = listLeaguesViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable("help_screen") {
                    val listHelpViewModel: ListHelpViewModel = hiltViewModel()
                    HelpScreen(
                        navController = navController,
                        helpViewModel = listHelpViewModel,
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




