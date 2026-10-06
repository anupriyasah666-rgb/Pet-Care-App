package np.com.petcareapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import np.com.petcareapplication.ui.Screen
import np.com.petcareapplication.ui.auth.LoginScreen
import np.com.petcareapplication.ui.auth.ProfileScreen
import np.com.petcareapplication.ui.auth.RegisterScreen
import np.com.petcareapplication.ui.home.HomeScreen
import np.com.petcareapplication.ui.pet.*
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.PetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PetCareApplicationTheme {
                PetCareApp()
            }
        }
    }
}

@Composable
fun PetCareApp() {
    val navController = rememberNavController()

    // Shared across every screen in the nav graph
    val authViewModel: AuthViewModel = viewModel()
    val petViewModel: PetViewModel = viewModel()

    // Firebase Auth securely persists the session on the device, so a returning user
    // goes straight to the dashboard. Only users who are signed out see the Login screen.
    // The start destination is decided once, when the nav graph is first created.
    val startDestination = remember {
        if (authViewModel.user.value != null) Screen.Home.route else Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                // Go back to the existing Login screen instead of stacking a second one
                onNavigateToLogin = {
                    if (!navController.popBackStack()) navController.navigate(Screen.Login.route)
                },
                onRegisterSuccess = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                petViewModel = petViewModel,
                onPetClick = { petId ->
                    navController.navigate(Screen.PetDetail.createRoute(petId))
                },
                onAddPetClick = {
                    navController.navigate(Screen.AddPet.route)
                },
                onEditTaskClick = { taskId ->
                    navController.navigate(Screen.EditTask.createRoute(taskId))
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onLogout = {
                    petViewModel.clearSession()   // stop listeners + clear previous user's data
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                viewModel = authViewModel
            )
        }

        composable(Screen.AddPet.route) {
            AddPetScreen(
                onBack = { navController.popBackStack() },
                onPetAdded = { navController.popBackStack() },
                authViewModel = authViewModel,
                petViewModel = petViewModel
            )
        }

        composable(
            route = Screen.EditPet.route,
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            EditPetScreen(
                petId = petId,
                onBack = { navController.popBackStack() },
                onPetUpdated = { navController.popBackStack() },
                authViewModel = authViewModel,
                petViewModel = petViewModel
            )
        }

        composable(
            route = Screen.PetDetail.route,
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            PetDetailScreen(
                petId = petId,
                onBack = { navController.popBackStack() },
                onAddTaskClick = {
                    navController.navigate(Screen.AddTask.createRoute(petId))
                },
                onEditPetClick = { id ->
                    navController.navigate(Screen.EditPet.createRoute(id))
                },
                onEditTaskClick = { taskId ->
                    navController.navigate(Screen.EditTask.createRoute(taskId))
                },
                onViewExpensesClick = { id ->
                    navController.navigate(Screen.Expenses.createRoute(id))
                },
                onViewMedicalClick = { id ->
                    navController.navigate(Screen.MedicalRecords.createRoute(id))
                },
                authViewModel = authViewModel,
                petViewModel = petViewModel
            )
        }

        composable(
            route = Screen.AddTask.route,
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            AddTaskScreen(
                petId = petId,
                onBack = { navController.popBackStack() },
                onTaskAdded = { navController.popBackStack() },
                authViewModel = authViewModel,
                petViewModel = petViewModel
            )
        }

        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            EditTaskScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() },
                onTaskUpdated = { navController.popBackStack() },
                petViewModel = petViewModel
            )
        }

        composable(
            route = Screen.Expenses.route,
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            ExpenseScreen(
                petId = petId,
                onBack = { navController.popBackStack() },
                onAddExpenseClick = {
                    navController.navigate(Screen.AddExpense.createRoute(petId))
                }
            )
        }

        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            AddExpenseScreen(
                petId = petId,
                onBack = { navController.popBackStack() },
                onExpenseAdded = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MedicalRecords.route,
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            MedicalRecordScreen(
                petId = petId,
                onBack = { navController.popBackStack() },
                petViewModel = petViewModel,
                authViewModel = authViewModel
            )
        }
    }
}