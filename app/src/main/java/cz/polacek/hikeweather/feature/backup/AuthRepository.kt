package cz.polacek.hikeweather.feature.backup

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/** Prihlaseny uzivatel */
data class Account(val uid: String, val name: String?, val email: String?)

/**
 * Prihlaseni Google uctem pres Credential Manager a Firebase Authentication
 * Firebase pak podle uid oddeluje zalohy jednotlivych uzivatelu
 */
class AuthRepository(private val webClientId: String) {

    private val auth = FirebaseAuth.getInstance()

    private val _account = MutableStateFlow(auth.currentUser?.toAccount())
    val account: StateFlow<Account?> = _account.asStateFlow()

    init {
        auth.addAuthStateListener { _account.value = it.currentUser?.toAccount() }
    }

    /** zobrazi systemovy vyber Google uctu, potrebuje kontext aktivity */
    suspend fun signIn(activityContext: Context): Result<Account> = runCatching {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential

        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Neočekávaný typ přihlášení"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
        requireNotNull(user) { "Přihlášení se nezdařilo" }.toAccount()
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    private fun com.google.firebase.auth.FirebaseUser.toAccount() = Account(uid, displayName, email)
}
