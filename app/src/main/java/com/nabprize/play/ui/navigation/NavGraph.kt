package com.nabprize.play.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nabprize.play.config.FeatureFlags
import com.nabprize.play.data.AuthRepository
import com.nabprize.play.ui.ads.rememberInterstitialAdState
import com.nabprize.play.ui.auth.AuthViewModel
import com.nabprize.play.ui.auth.UserViewModel
import com.nabprize.play.ui.components.OfflineBanner
import com.nabprize.play.ui.components.rememberNetworkAvailable
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
    val isOnline = rememberNetworkAvailable()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val bottomTabs = listOf(
        BottomTab(Routes.HOME, "Home", Icons.Default.Home),
        BottomTab(Routes.PRACTICE, "Play & Earn", Icons.Default.SportsEsports),
        BottomTab(Routes.REWARDS, "Rewards", Icons.Default.EmojiEvents)
    )
    // All MVP destinations keep the primary navigation visible. Practice can still scroll
    // its board/content within the Scaffold area on compact devices.
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    LaunchedEffect(userState.error, userState.successMessage) {
        val message = userState.error ?: userState.successMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            if (userState.error != null) userViewModel.clearError()
            if (userState.successMessage != null) userViewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = { if (!isOnline) OfflineBanner() },
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
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                todayMatchesPlayed = userState.todayMatchesPlayed,
                todayCoinsEarned = userState.todayCoinsEarned,
                checkInDay = userState.checkInDay,
                isCheckedInToday = userState.isCheckedInToday,
                nextCheckInDay = userState.nextCheckInDay,
                onAvatarClick          = { navController.navigate(Routes.PROFILE) },
                onNextAchievementClick = { navController.navigate(Routes.REWARDS) },
                onPracticeClick        = { navController.navigate(Routes.PRACTICE) },
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
                totalPracticeMatches = userState.profile.totalPracticeMatches,
                lifetimeCoinsEarned = userState.profile.lifetimeCoinsEarned,
                onBack = { navController.popBackStack() },
                onLogout = {
                    userViewModel.clearSession()
                    authViewModel.signOut()
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
                onClaimPracticeReward = { isWin, boxesCaptured, onComplete ->
                    userViewModel.recordPracticeResult(isWin, boxesCaptured, onComplete)
                },
                onClaimBonusCoins = { onComplete ->
                    userViewModel.addCoins(10, onComplete)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MATCHMAKING) {
            if (!FeatureFlags.CHALLENGE_ENABLED) {
                ChallengeComingSoonScreen()
            } else if (userState.profile.tickets > 0) {
                MatchmakingScreen(
                    onBack = { navController.popBackStack() },
                    playerName = userState.profile.displayName.ifBlank { userState.profile.username }.ifBlank { "Player" },
                    onMatchResult = { matchId, isWin -> userViewModel.updateMatchResult(matchId, isWin) }
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
                // Online results are settled by MatchmakingScreen with the server match ID.
                // This legacy route has no match ID, so it must not write a duplicate reward.
                onResultRecorded = { }
            )
        }
    }
    }
}

@Composable
private fun ChallengeComingSoonScreen() {
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Challenge Coming Soon",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Live 1v1 is temporarily disabled.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
