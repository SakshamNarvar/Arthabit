package com.nstrange.arthabit.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nstrange.arthabit.ui.screens.home.HomeScreen
import com.nstrange.arthabit.ui.screens.login.LoginScreen
import com.nstrange.arthabit.ui.screens.profile.ProfileScreen
import com.nstrange.arthabit.ui.screens.signup.SignupScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Login.route,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
    ) {
        composable(Routes.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Routes.SignUp.route)
                }
            )
        }

        composable(Routes.SignUp.route) {
            SignupScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigateUp()
                }
            )
        }

        composable(Routes.Home.route) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Routes.Profile.route)
                }
            )
        }

        composable(Routes.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

