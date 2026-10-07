package np.com.petcareapplication.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import np.com.petcareapplication.model.Pet
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.model.Expense
import np.com.petcareapplication.model.MedicalRecord
import java.util.UUID

/**
 * Reads and saves all the pet data in Firestore: pets, tasks, expenses and health
 * records. The ViewModels call these functions instead of using Firebase directly.
 */
class PetRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val petsCollection = firestore.collection("pets")
    private val tasksCollection = firestore.collection("tasks")
    private val expensesCollection = firestore.collection("expenses")
    private val recordsCollection = firestore.collection("medical_records")

    // Uploads a photo from the gallery and gives back its link. This needs Firebase
    // Storage, which is on the paid Blaze plan, so for now photos are added as links instead.
    suspend fun uploadPetImage(imageUri: Uri): String {
        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("pet_images/$fileName")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    // ---------- Pets ----------

    // Gives a live list of this owner's pets. The snapshot listener sends a fresh list
    // whenever a pet is added, edited or deleted, and awaitClose removes the listener
    // once nothing is listening any more.
    fun getPets(ownerId: String): Flow<List<Pet>> = callbackFlow {
        val subscription = petsCollection
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val pets = snapshot?.toObjects(Pet::class.java) ?: emptyList()
                trySend(pets)
            }
        awaitClose { subscription.remove() }
    }

    // Firestore makes up the document ID for a new pet
    suspend fun addPet(pet: Pet) {
        petsCollection.add(pet).await()
    }

    // Overwrites the pet with the same ID
    suspend fun updatePet(pet: Pet) {
        petsCollection.document(pet.id).set(pet).await()
    }

    // Removes the pet itself. Its tasks, expenses and health records are removed
    // separately in cleanupPetData().
    suspend fun deletePet(petId: String) {
        petsCollection.document(petId).delete().await()
    }

    // Runs after a pet is deleted, so its tasks, expenses and health records
    // aren't left behind in the database with no pet attached.
    suspend fun cleanupPetData(petId: String) {
        // Tasks
        deleteRoutine(petId)

        // Expenses
        val expenses = expensesCollection.whereEqualTo("petId", petId).get().await()
        for (doc in expenses.documents) { doc.reference.delete().await() }

        // Health records
        val records = recordsCollection.whereEqualTo("petId", petId).get().await()
        for (doc in records.documents) { doc.reference.delete().await() }
    }

    // ---------- Tasks ----------

    // Live list of one pet's tasks, used on the Pet Detail screen
    fun getTasks(petId: String): Flow<List<CareTask>> = callbackFlow {
        val subscription = tasksCollection
            .whereEqualTo("petId", petId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.toObjects(CareTask::class.java) ?: emptyList()
                trySend(tasks)
            }
        awaitClose { subscription.remove() }
    }

    // Live list of every task the owner has, across all their pets.
    // This is what fills the "Today's Routine" list on the Home screen.
    fun getAllTasks(ownerId: String): Flow<List<CareTask>> = callbackFlow {
        val subscription = tasksCollection
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.toObjects(CareTask::class.java) ?: emptyList()
                trySend(tasks)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addTask(task: CareTask) {
        tasksCollection.add(task).await()
    }

    // Used both for editing a task and for ticking it off
    suspend fun updateTask(task: CareTask) {
        tasksCollection.document(task.id).set(task).await()
    }

    suspend fun deleteTask(taskId: String) {
        tasksCollection.document(taskId).delete().await()
    }

    // Deletes every task for one pet. Used by "Clear All Tasks" and when a pet is deleted.
    suspend fun deleteRoutine(petId: String) {
        val tasks = tasksCollection.whereEqualTo("petId", petId).get().await()
        for (doc in tasks.documents) {
            doc.reference.delete().await()
        }
    }

    // ---------- Expenses ----------

    // Live list of one pet's expenses
    fun getExpenses(petId: String): Flow<List<Expense>> = callbackFlow {
        val subscription = expensesCollection
            .whereEqualTo("petId", petId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                trySend(expenses)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addExpense(expense: Expense) {
        expensesCollection.add(expense).await()
    }

    suspend fun deleteExpense(expenseId: String) {
        expensesCollection.document(expenseId).delete().await()
    }

    // ---------- Health records ----------

    // Live list of one pet's health records
    fun getMedicalRecords(petId: String): Flow<List<MedicalRecord>> = callbackFlow {
        val subscription = recordsCollection
            .whereEqualTo("petId", petId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(MedicalRecord::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addMedicalRecord(record: MedicalRecord) {
        recordsCollection.add(record).await()
    }

    suspend fun deleteMedicalRecord(recordId: String) {
        recordsCollection.document(recordId).delete().await()
    }
}