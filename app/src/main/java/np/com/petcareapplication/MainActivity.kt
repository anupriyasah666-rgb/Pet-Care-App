package np.com.petcareapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    // Always force a logout when the app process starts.
    LaunchedEffect(Unit) {
        authViewModel.logout()
    }

    val user by authViewModel.user.collectAsState()

    // Login is always the first screen the user sees
    val startDestination = Screen.Login.route

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
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
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
                    navController.navigate("add_expense/$petId")
                }
            )
        }

        composable(
            route = "add_expense/{petId}",
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}