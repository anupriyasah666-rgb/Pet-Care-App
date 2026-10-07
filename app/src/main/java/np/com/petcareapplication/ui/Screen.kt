package np.com.petcareapplication.ui

// Every screen in the app and its navigation route.
// MainActivity's NavHost uses these instead of typing the route strings by hand
sealed class Screen(val route: String) {
    // Screens that don't need any extra information
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object AddPet : Screen("add_pet")
    object Profile : Screen("profile")

    // The screens below need an id in the route, e.g. "edit_pet/abc123".
    // createRoute() fills the id in, so the route text is only written in one place
    object EditPet : Screen("edit_pet/{petId}") {
        fun createRoute(petId: String) = "edit_pet/$petId"
    }

    object PetDetail : Screen("pet_detail/{petId}") {
        fun createRoute(petId: String) = "pet_detail/$petId"
    }

    // New tasks belong to a pet, so this route takes the pet's id
    object AddTask : Screen("add_task/{petId}") {
        fun createRoute(petId: String) = "add_task/$petId"
    }

    // Editing only needs the task's own id
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: String) = "edit_task/$taskId"
    }

    object Expenses : Screen("expenses/{petId}") {
        fun createRoute(petId: String) = "expenses/$petId"
    }

    object AddExpense : Screen("add_expense/{petId}") {
        fun createRoute(petId: String) = "add_expense/$petId"
    }

    // Health records for one pet
    object MedicalRecords : Screen("medical_records/{petId}") {
        fun createRoute(petId: String) = "medical_records/$petId"
    }
}