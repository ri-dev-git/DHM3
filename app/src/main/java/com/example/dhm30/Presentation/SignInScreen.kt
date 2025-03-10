package com.example.dhm30.Presentation

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dhm30.R
import com.example.dhm30.Utils.CLIENT_ID
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth

import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID



@Preview()
@Composable
fun preview(){
    val NavController= rememberNavController()
    SignInScreen(NavController)
}


@Composable
fun SignInScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {




        val context = LocalContext.current




        val onClick: () ->Unit  = {
            val credentialManager = CredentialManager.create(context)
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(CLIENT_ID)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            coroutineScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context,
                    )
                    val credential = result.credential
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken

                    // Sign in with Firebase using the ID token
                    signInWithGoogle(context, googleIdToken)

                    Toast.makeText(context, "You are signed in!", Toast.LENGTH_SHORT).show()

                    // Navigate to the home screen and clear the back stack
                    navController.navigate("home") {
                        popUpTo("sign_in") { inclusive = true } // Clear the back stack
                    }
                } catch (e: GetCredentialException) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                } catch (e: GoogleIdTokenParsingException) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        Image(
            painter = painterResource(id = R.drawable.app_logo), // Reference your logo drawable here
            contentDescription = "App Logo", // A description for accessibility
            modifier = Modifier.size(150.dp) // Adjust the size as per your requirement
        )


        IconButton(
            onClick = onClick, // Set the onClick listener
            modifier = Modifier.size(180.dp) // Set the size of the button


        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_signin), // Replace with your drawable
                contentDescription = "Google Sign-In Icon",
                tint = Color.Unspecified, // Preserve original colors of the icon
                modifier = Modifier.size(180.dp) // Set the icon size

            )
        }
    }
}

suspend fun signInWithGoogle(context: Context,idToken: String) {
    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
    Firebase.auth.signInWithCredential(firebaseCredential).await()
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
    val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()

    // Get the current user
    val firebaseUser = authResult.user

    // Access the email ID
    val email = firebaseUser?.email
    Log.d("emailgfb",email.toString())

    // You can now use the email ID as needed
    if (email != null) {
        // For example, store the email in SharedPreferences or use it in your app
        storeEmail(context, email)
    }
    storeIdToken(context,firebaseUid as String)
    saveInstallationTime(context)
}
fun storeEmail(context: Context, email: String) {
    val sharedPreferences = context.getSharedPreferences("My_App_Prefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("user_email", email)
    editor.apply()
}
fun storeIdToken(context: Context, idToken: String) {
    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString("uid_token", idToken)
        apply()
    }
}


fun saveInstallationTime(context: Context) {
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putLong("installation_time", System.currentTimeMillis())
        apply()
    }
}

fun getInstallationTime(context: Context): Long? {
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return sharedPref.getLong("installation_time", 0)
}