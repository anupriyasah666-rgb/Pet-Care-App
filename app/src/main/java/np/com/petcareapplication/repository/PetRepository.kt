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
 * PetRepository manages Firestore data and Firebase Storage for pet images.
 * This repository handles all data for Pets, Tasks, Expenses, and Medical Records.
 */
class PetRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val petsCollection = firestore.collection("pets")
    private val tasksCollection = firestore.collection("tasks")
    private val expensesCollection = firestore.collection("expenses")
    private val recordsCollection = firestore.collection("medical_records")

    /**
     * Uploads an image to Firebase Storage and returns the download URL.
     */
    suspend fun uploadPetImage(imageUri: Uri): String {
        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("pet_images/$fileName")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    // --- PET MANAGEMENT ---

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

    suspend fun addPet(pet: Pet) {
        petsCollection.add(pet).await()
    }

    suspend fun updatePet(pet: Pet) {
        petsCollection.document(pet.id).set(pet).await()
    }

    /**
     * CORE REQUIREMENT: Delete items - remove unwanted pets.
     */
    suspend fun deletePet(petId: String) {
        // Delete the pet profile primary record
        petsCollection.document(petId).delete().await()
    }

    /**
     * Performs background cleanup for a deleted pet.
     */
    suspend fun cleanupPetData(petId: String) {
        // Clear the routine (tasks)
        deleteRoutine(petId) 

        // Clear expenses
        val expenses = expensesCollection.whereEqualTo("petId", petId).get().await()
        for (doc in expenses.documents) { doc.reference.delete().await() }

        // Clear medical records
        val records = recordsCollection.whereEqualTo("petId", petId).get().await()
        for (doc in records.documents) { doc.reference.delete().await() }
    }

    // --- TASK & ROUTINE MANAGEMENT ---

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

    /**
     * Fulfills "Consolidation" requirement.
     * Fetches all tasks across ALL pets for a specific owner.
     */
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

    suspend fun updateTask(task: CareTask) {
        tasksCollection.document(task.id).set(task).await()
    }

    suspend fun deleteTask(taskId: String) {
        tasksCollection.document(taskId).delete().await()
    }

    /**
     * CORE REQUIREMENT: Delete care routines.
     */
    suspend fun deleteRoutine(petId: String) {
        val tasks = tasksCollection.whereEqualTo("petId", petId).get().await()
        for (doc in tasks.documents) {
            doc.reference.delete().await()
        }
    }

    // --- EXPENSE TRACKING ---

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

    // --- MEDICAL RECORDS ---

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
