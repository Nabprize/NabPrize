package com.nabprize.play.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nabprize.play.data.AuthRepository
import com.nabprize.play.ui.ads.rememberInterstitialAdState
import com.nabprize.play.ui.auth.AuthViewModel
import com.nabprize.play.ui.auth.UserViewModel
import com.nabprize.play.ui.screens.LoginSignupScreen
import com.nabprize.play.ui.screens.home.HomeScreen
import com.nabprize.play.ui.screens.matchmaking.MatchmakingScreen
import com.nabprize.play.ui.screens.matchresult.MatchResultScreen
import com.nabprize.play.ui.screens.practice.PracticeScreen
import com.nabprize.play.ui.screens.profile.ProfileScreen
import com.nabprize.play.ui.screens.rewards.RewardsScreen
import com.nabprize.play.ui.theme.CardWhite
import com.nabprize.play.ui.theme.PrimaryOrange
import com.nabprize.play.ui.theme.TextSecondary

// Helper: safely find Activity from any Context (Application/Wrapper-based contexts)
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val REWARDS = "rewards"
    const val PRACTICE = "practice"
    const val MATCHMAKING = "matchmaking"
    const val MATCH_RESULT = "match_result"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun NabPrizeNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val startDestination = if (authViewModel.state.value.isLoggedIn) Routes.HOME else Routes.LOGIN
    val userState by userViewModel.state.collectAsState()
    val interstitialAdState = rememberInterstitialAdState()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val bottomTabs = listOf(
        BottomTab(Routes.HOME, "Home", Icons.Default.Home),
        BottomTab(Routes.PRACTICE, "Play & Earn", Icons.Default.SportsEsports),
        BottomTab(Routes.MATCHMAKING, "Challenge", Icons.Outlined.LocalActivity),
        BottomTab(Routes.REWARDS, "Rewards", Icons.Default.EmojiEvents)
    )
    // Practice needs the full height for the board and its entry content.
    val showBottomBar = currentRoute in bottomTabs.map { it.route } && currentRoute != Routes.PRACTICE

    LaunchedEffect(userState.error, userState.successMessage) {
        val message = userState.error ?: userState.successMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            if (userState.error != null) userViewModel.clearError()
            if (userState.successMessage != null) userViewModel.clearSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = CardWhite,
                    tonalElevation = 0.dp
                ) {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryOrange,
                                selectedTextColor = PrimaryOrange,
                                indicatorColor = PrimaryOrange.copy(alpha = 0.14f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = androidx.compose.ui.Modifier.padding(contentPadding)
    ) {
        composable(Routes.LOGIN) {
            LoginSignupScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    userViewModel.fetchProfile()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                username = userState.profile.displayName.ifBlank { userState.profile.username }.ifBlank { "Player" },
                npCoins = userState.profile.npCoins,
                tickets = userState.profile.tickets,
                totalWins = userState.profile.totalWins,
                todayMatchesPlayed = userState.todayMatchesPlayed,
                todayCoinsEarned = userState.todayCoinsEarned,
                checkInDay = userState.checkInDay,
                isCheckedInToday = userState.isCheckedInToday,
                nextCheckInDay = userState.nextCheckInDay,
                onAvatarClick          = { navController.navigate(Routes.PROFILE) },
                onNextAchievementClick = { navController.navigate(Routes.REWARDS) },
                onPracticeClick        = { navController.navigate(Routes.PRACTICE) },
                onChallengeClick       = {
                    if (userState.profile.tickets > 0) {
                        navController.navigate(Routes.MATCHMAKING)
                    }
                },
                onRewardsClick         = { navController.navigate(Routes.REWARDS) },
                onDailyCheckinClick    = {
                    // Use findActivity() instead of direct cast to avoid silent null failures
                    val activity = context.findActivity()
                    if (activity != null) {
                        interstitialAdState.show(activity) {
                            userViewModel.dailyCheckIn()
                        }
                    } else {
                        // Fallback if activity not found
                        userViewModel.dailyCheckIn()
                    }
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                displayName = userState.profile.displayName,
                username = userState.profile.username,
                email = userState.profile.email,
                totalWins = userState.profile.totalWins,
                totalLosses = userState.profile.totalLosses,
                npCoins = userState.profile.npCoins,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REWARDS) {
            RewardsScreen(
                npCoins = userState.profile.npCoins,
                onRedeem = { tier, p1, p2, cb ->
                    userViewModel.redeemReward(tier.id, tier.name, tier.cost.toLong(), p1, p2, cb)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRACTICE) {
            PracticeScreen(
                tickets = userState.profile.tickets,
                dailyTicketsEarned = userState.profile.dailyTicketsEarned,
                adsTowardTicket = userState.adsTowardNextTicket,
                isDailyCapReached = userState.isDailyCapReached,
                onPracticeResult = { isWin, boxesCaptured ->
                    userViewModel.recordPracticeResult(isWin, boxesCaptured)
                },
                onWatchTicketAd = {
                    userViewModel.watchRewardedAdForTicket()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MATCHMAKING) {
            if (userState.profile.tickets > 0) {
                MatchmakingScreen(
                    onBack = { navController.popBackStack() },
                    playerName = userState.profile.displayName.ifBlank { userState.profile.username }.ifBlank { "Player" },
                    onMatchResult = { isWin -> userViewModel.updateMatchResult(isWin) }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Routes.MATCH_RESULT) {
            MatchResultScreen(
                onHome = { navController.popBackStack() },
                onPlayAgain = { navController.navigate(Routes.MATCHMAKING) },
                onPracticeAndEarn = { navController.navigate(Routes.PRACTICE) },
                onResultRecorded = { isWin ->
                    userViewModel.updateMatchResult(isWin)
                }
            )
        }
    }
    }
}
