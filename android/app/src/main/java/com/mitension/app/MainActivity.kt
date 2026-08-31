package com.mitension.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mitension.app.data.RoomMeasurementsRepository
import com.mitension.app.sync.SupabaseSession
import com.mitension.app.sync.SyncScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var authState by mutableStateOf<AuthState>(AuthState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MiTensionApplication
        setContent {
            MaterialTheme {
                when (val state = authState) {
                    AuthState.Loading -> LoadingScreen()
                    is AuthState.SignedOut -> SignInScreen(app.authManager.isConfigured, state.error) { email, password ->
                        authState = AuthState.Loading
                        lifecycleScope.launch {
                            authState = runCatching { app.authManager.signIn(email, password) }
                                .fold({ session ->
                                    SyncScheduler.enqueueNow(applicationContext)
                                    AuthState.SignedIn(session)
                                }, { AuthState.SignedOut(it.message ?: "No se pudo iniciar sesión") })
                        }
                    }
                    is AuthState.SignedIn -> MiTensionApp(measurementsViewModel(app, state.session))
                }
            }
        }
        authState = app.authManager.restoredSession()?.let(AuthState::SignedIn) ?: AuthState.SignedOut()
    }

    private fun measurementsViewModel(app: MiTensionApplication, session: SupabaseSession): MeasurementsViewModel {
        val factory = MeasurementsViewModelFactory(RoomMeasurementsRepository(app.database.measurementsDao(), session.userId) {
            SyncScheduler.enqueueNow(applicationContext)
        })
        return ViewModelProvider(this, factory)["measurements-${session.userId}", MeasurementsViewModel::class.java]
    }
}

private sealed interface AuthState {
    object Loading : AuthState
    data class SignedOut(val error: String? = null) : AuthState
    data class SignedIn(val session: SupabaseSession) : AuthState
}

@Composable
private fun LoadingScreen() = Column(
    Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
) {
    CircularProgressIndicator()
    Text("Abriendo miTensión…", Modifier.padding(top = 12.dp))
}

@Composable
private fun SignInScreen(configured: Boolean, error: String?, onSignIn: (String, String) -> Unit) {
    var email by androidx.compose.runtime.remember { mutableStateOf("") }
    var password by androidx.compose.runtime.remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Autorizar este dispositivo", style = MaterialTheme.typography.headlineSmall)
        Text("Inicia sesión una vez con la cuenta ya aprovisionada.", Modifier.padding(vertical = 12.dp))
        if (!configured) Text("Falta configurar SUPABASE_URL y SUPABASE_PUBLISHABLE_KEY.", color = MaterialTheme.colorScheme.error)
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Button(
            onClick = { onSignIn(email, password) },
            enabled = configured && email.isNotBlank() && password.isNotEmpty(),
            modifier = Modifier.padding(top = 12.dp),
        ) { Text("Iniciar sesión") }
    }
}
