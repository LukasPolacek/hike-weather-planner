package com.example.tmaa_hikeweatherplanner

import com.example.tmaa_hikeweatherplanner.ui.theme.TMAAHikeWeatherPlannerTheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
//Layouty,UI komponenty
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
//Sprava stavu,livedata
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
//Navigace,serializace
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import android.util.Log
import android.os.Bundle

//Definice tras
@Serializable
object WelcomeRoute

@Serializable
data class FormRoute(val initialValue: String = "")

@Serializable
data class SummaryRoute(
    val name: String,
    val birthDate: String,
    val destination: String,
    val difficulty: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Lifecycle", "onCreate")
        enableEdgeToEdge()
        setContent {
            TMAAHikeWeatherPlannerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        HikeNavigation()
                    }
                }
            }
        }
    }

    //Lifecycle
    override fun onStart() { super.onStart(); Log.i("Lifecycle", "onStart") }
    override fun onResume() { super.onResume(); Log.i("Lifecycle", "onResume") }
    override fun onPause() { super.onPause(); Log.i("Lifecycle", "onPause") }
    override fun onStop() { super.onStop(); Log.i("Lifecycle", "onStop") }
    override fun onRestart() { super.onRestart(); Log.i("Lifecycle", "onRestart") }
    override fun onDestroy() { super.onDestroy(); Log.i("Lifecycle", "onDestroy") }
}

@Composable
fun HikeNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = WelcomeRoute) {
        composable<WelcomeRoute> {
            //Sledovani navratove h. pres savedStateHandle
            val consentStatus = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.getLiveData<Boolean>("consent_result")
                ?.observeAsState()

            WelcomeScreen(
                consent = consentStatus?.value,
                onStartClick = { navController.navigate(FormRoute()) }
            )
        }

        composable<FormRoute> {
            FormScreen(onNavigate = { n, d, dest, diff ->
                navController.navigate(SummaryRoute(n, d, dest, diff))
            })
        }

        composable<SummaryRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<SummaryRoute>()
            SummaryScreen(
                name = args.name,
                birthDate = args.birthDate,
                destination = args.destination,
                difficulty = args.difficulty,
                onBack = { agreed ->
                    //Ulozeni dat pro prvni obrazovku
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("consent_result", agreed)
                    //Vraceni na welcomeRoute
                    navController.popBackStack(route = WelcomeRoute, inclusive = false)
                }
            )
        }
    }
}

@Composable
fun WelcomeScreen(consent: Boolean?, onStartClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Hike Weather Planner", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(20.dp))

        if (consent != null) {
            Text("Souhlas s podmínkami: ${if (consent) "ANO" else "NE"}")
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(onClick = onStartClick) { Text("Vytvořit nový plán") }
    }
}

@Composable
fun FormScreen(onNavigate: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var isAgreed by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Lehká", "Střední", "Náročná")
    var selectedDiff by remember { mutableStateOf(options[0]) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Zadejte údaje o výletníkovi", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Jméno") })
        OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Datum narození") })

        Spacer(modifier = Modifier.height(24.dp))

        Text("Zadejte údaje o výletě", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("Místo") })

        Spacer(modifier = Modifier.height(16.dp))

        //Spinner
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("Náročnost: $selectedDiff")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedDiff = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isAgreed, onCheckedChange = { isAgreed = it })
            Text("Souhlasím s podmínkami")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onNavigate(name, birthDate, destination, selectedDiff) },
            enabled = isAgreed && name.isNotBlank() && destination.isNotBlank()
        ) { Text("Zobrazit souhrn") }
    }
}

@Composable
fun SummaryScreen(name: String, birthDate: String, destination: String, difficulty: String, onBack: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Souhrn plánu", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Výletník: $name ($birthDate)")
        Text("Cíl cesty: $destination")
        Text("Náročnost: $difficulty")

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { onBack(true) }) {
            Text("Potvrdit a vrátit se")
        }
    }
}