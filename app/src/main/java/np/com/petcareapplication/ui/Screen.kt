package np.com.petcareapplication.ui

/**
 * Sealed class defining the navigation routes for the application.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object AddPet : Screen("add_pet")
    object Profile : Screen("profile")
    
    object EditPet : Screen("edit_pet/{petId}") {
        fun createRoute(petId: String) = "edit_pet/$petId"
    }
    
    object PetDetail : Screen("pet_detail/{petId}") {
        fun createRoute(petId: String) = "pet_detail/$petId"
    }
    
    object AddTask : Screen("add_task/{petId}") {
        fun createRoute(petId: String) = "add_task/$petId"
    }
    
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: String) = "edit_task/$taskId"
    }
    
    object Expenses : Screen("expenses/{petId}") {
        fun createRoute(petId: String) = "expenses/$petId"
    }

    object AddExpense : Screen("add_expense/{petId}") {
        fun createRoute(petId: String) = "add_expense/$petId"
    }

    // NEW: Route for Medical Records Screen
    object MedicalRecords : Screen("medical_records/{petId}") {
        fun createRoute(petId: String) = "medical_records/$petId"
    }
}
