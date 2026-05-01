package cloud.scoreprof.app.ui.navigation

sealed class Screen(val route: String) {
    // Defines the route for your screen that lists competitions/leagues
    object LeagueScreen : Screen("league_screen")

    // Defines the route for the screen that lists matches for a specific competition
    object ListMatchesScreen : Screen("match_screen")
}