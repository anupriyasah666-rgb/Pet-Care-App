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

// The app's only Activity. Every screen is a Composable shown inside it
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let the app draw behind the status bar and navigation bar
        enableEdgeToEdge()
        setContent {
            PetCareApplicationTheme {
                PetCareApp()
            }
        }
    }
}

// Sets up navigation: which screen each route opens and where each button goes
@Composable
fun PetCareApp() {
    val navController = rememberNavController()

    // Created once here and passed to every screen, so they all share the same data
    val authViewModel: AuthViewModel = viewModel()
    val petViewModel: PetViewModel = viewModel()

    // Firebase remembers the login on the phone, so someone who is already signed in goes straight to Home.
    // Only signed-out users see the Login screen. This is worked out once, when the app starts
    val startDestination = remember {
        if (authViewModel.user.value != null) Screen.Home.route else Screen.Login.route
    }

    // One NavHost holds every screen in the app
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    // Remove Login from the back stack, so pressing back on Home doesn't go back to Login
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                // Go back to the Login screen that's already open instead of opening a second one
                onNavigateToLogin = {
                    if (!navController.popBackStack()) navController.navigate(Screen.Login.route)
                },
                onRegisterSuccess = {
                    // Firebase signs a new user in straight away, so sign them out
                    // and send them to Login to sign in with their new details
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
                    petViewModel.clearSession()   // stop the live listeners and clear the last user's data
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

        // The routes below carry an id (petId or taskId), which is read from the route and passed to the screen
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

        // The expense screens make their own ExpenseViewModel. That's fine because the list updates live from Firestore
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