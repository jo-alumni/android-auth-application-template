package com.example.authapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.core.navigation.TopLevelDestination
import com.example.authapplication.core.ui.AppTopBar
import com.example.authapplication.navigation.AppBottomBar
import com.example.authapplication.navigation.AppNavHost

@Composable
fun AuthApplicationApp(appViewModel: AppViewModel = hiltViewModel()) {
    val authState by appViewModel.authState.collectAsStateWithLifecycle()
    when (val state = authState) {
        AuthUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is AuthUiState.Ready -> {
            val navController = rememberNavController()
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            val showBottomBar = TopLevelDestination.entries.any { destination ->
                currentDestination?.hasRoute(destination.route::class) == true
            }

            LaunchedEffect(navController) {
                appViewModel.event.collect { event ->
                    when (event) {
                        AppEvent.NavigateLogin -> {
                            navController.navigate(AppRoute.AuthGraph) {
                                popUpTo(AppRoute.MainGraph) { inclusive = true }
                            }
                        }
                    }
                }
            }

            Scaffold(
                topBar = {
                    if (showBottomBar) {
                        AppTopBar(
                            onLogoutClick = { appViewModel.logout() },
                        )
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        AppBottomBar(navController = navController, currentDestination = currentDestination)
                    }
                },
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    startDestination = if (state.isAuthenticated) AppRoute.MainGraph else AppRoute.AuthGraph,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
