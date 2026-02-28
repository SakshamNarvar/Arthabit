package com.nstrange.arthabit.navigation

/**
 * Sealed class defining all navigation routes in the app.
 */
sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object SignUp : Routes("signup")
    data object Home : Routes("home")
    data object Profile : Routes("profile")
}
