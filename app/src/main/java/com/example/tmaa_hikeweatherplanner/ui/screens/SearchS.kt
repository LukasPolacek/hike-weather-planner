package com.example.tmaa_hikeweatherplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tmaa_hikeweatherplanner.data.SavedLocation
//obrazovka pro vyhledavani a pridavani novych mist do seznamu
@Composable
fun SearchS(
    recentLocations: List<SavedLocation>,
    onBack: () -> Unit,
    onLocationSaved: (String, String, String) -> Unit
) {
    //stav pro aktualne zadany text v poli vyhledavani
    var cityQuery by remember { mutableStateOf("") }
    //stav pro rizeni zobrazeni menu narocnosti
    var expanded by remember { mutableStateOf(false) }
    //definice moznych urovni narocnosti vyletu
    val options = listOf("Lehká", "Střední", "Náročná")
    //stav pro vybranou narocnost
    var selectedDiff by remember { mutableStateOf(options[0]) }
    //stav pro scrollovani
    val scrollState = rememberScrollState()
    //hlavni kontejner
    Column(
        //nadpis obrazovky sjednoceny s dashboardem
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hledat jiné místo", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        //textove pole pro zadavani nazvu lokality
        OutlinedTextField(
            value = cityQuery,
            onValueChange = { cityQuery = it },
            label = { Text("Zadejte město") },
            modifier = Modifier.fillMaxWidth()
        )

        if (cityQuery.isNotBlank()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                //vnitrni rozlozeni karty s informacemi o pocasi
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cityQuery, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Náročnost:", style = MaterialTheme.typography.labelMedium)
                        //volba narocnosti pres tlacitko a dropdown
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.height(36.dp)) {
                                Text(selectedDiff, fontSize = 14.sp)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                options.forEach { opt ->
                                    DropdownMenuItem(text = { Text(opt) }, onClick = { selectedDiff = opt; expanded = false })
                                }
                            }
                        }
                    }
                    //jenom napevno data pocasi pro lokalitu, nez bude zprovozneny
                    Column(horizontalAlignment = Alignment.End) {
                        Text("18°C", style = MaterialTheme.typography.displaySmall)
                        Text("Polojasno", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                //tlacitko pro ulozeni vyletu a vymazani vyhledavani
                Button(
                    onClick = { onLocationSaved(cityQuery, selectedDiff, "18°C"); cityQuery = "" },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text("Uložit do seznamu") }
            }
        }
        //zobrazeni sekce historie pokud existuji predchozi hledani
        if (recentLocations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            Text("Nedávno hledané", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            //vypis karet s historii poslednich hledani
            recentLocations.forEach { loc ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(loc.city, style = MaterialTheme.typography.titleMedium)
                            Text("Náročnost: ${loc.difficulty}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(loc.weather)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        //navrat na predchozi obrazovku
        TextButton(onClick = onBack) { Text("Hotovo, zpět na Dashboard") }
    }
}