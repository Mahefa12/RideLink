package com.app.ridelink.data.repository

import com.app.ridelink.data.dao.UserDao
import com.app.ridelink.data.model.User
import com.app.ridelink.auth.GoogleSignInManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val googleSignInManager: GoogleSignInManager
) {
    fun getCurrentUser(): Flow<User?> {
        val currentUser = firebaseAuth.currentUser
        return if (currentUser != null) {
            userDao.getUserById(currentUser.uid)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }

    suspend fun getCurrentUserSync(): User? {
        val currentUser = firebaseAuth.currentUser
        return if (currentUser != null) {
            userDao.getUserByIdSync(currentUser.uid)
        } else {
            null
        }
    }

    suspend fun signInWithGoogle(): Result<User> {
        return try {
            val intent = googleSignInManager.getSignInIntent()
            // This method will be called from UI layer with the result
            Result.failure(Exception("Google Sign-In requires UI interaction. Use signInWithGoogle(account) instead."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getGoogleSignInIntent() = googleSignInManager.getSignInIntent()
    
    suspend fun handleGoogleSignInResult(data: android.content.Intent?): Result<User> {
        return try {
            val accountResult = googleSignInManager.handleSignInResult(data)
            if (accountResult.isSuccess) {
                val account = accountResult.getOrThrow()
                val authResult = signInWithGoogle(account)
                when (authResult) {
                    is AuthResult.Success -> Result.success(authResult.user)
                    is AuthResult.Error -> Result.failure(Exception(authResult.message))
                    is AuthResult.Loading -> Result.failure(Exception("Authentication in progress"))
                }
            } else {
                Result.failure(accountResult.exceptionOrNull() ?: Exception("Google Sign-In failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    phoneNumber = firebaseUser.phoneNumber,
                    isEmailVerified = firebaseUser.isEmailVerified
                )

                userDao.insertUser(user)
                saveUserToFirestore(user)
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Authentication failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val user = userDao.getUserByIdSync(firebaseUser.uid)
                    ?: getUserFromFirestore(firebaseUser.uid)
                    ?: User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: email,
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        phoneNumber = firebaseUser.phoneNumber,
                        isEmailVerified = firebaseUser.isEmailVerified
                    )
                
                userDao.insertUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailPassword(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val user = getUserFromFirestore(firebaseUser.uid)
                if (user != null) {
                    userDao.insertUser(user)
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("User data not found")
                }
            } else {
                AuthResult.Error("Authentication failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun registerWithEmail(email: String, password: String, displayName: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    photoUrl = null,
                    phoneNumber = null,
                    isEmailVerified = firebaseUser.isEmailVerified
                )

                userDao.insertUser(user)
                saveUserToFirestore(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmailPassword(email: String, password: String, displayName: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    photoUrl = null,
                    phoneNumber = null,
                    isEmailVerified = firebaseUser.isEmailVerified
                )

                userDao.insertUser(user)
                saveUserToFirestore(user)
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Registration failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            userDao.deleteUserById(currentUser.uid)
        }
    }

    private suspend fun saveUserToFirestore(user: User) {
        try {
            firestore.collection("users")
                .document(user.id)
                .set(user)
                .await()
        } catch (e: Exception) {
        }
    }

    private suspend fun getUserFromFirestore(userId: String): User? {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            userDao.updateUser(user)
            saveUserToFirestore(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                currentUser.updateEmail(newEmail).await()
                val user = userDao.getUserByIdSync(currentUser.uid)
                if (user != null) {
                    val updatedUser = user.copy(email = newEmail, isEmailVerified = false)
                    userDao.updateUser(updatedUser)
                    saveUserToFirestore(updatedUser)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("No authenticated user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null && user.email != null) {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, currentPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No authenticated user or email"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}