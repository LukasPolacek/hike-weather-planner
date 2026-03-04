package com.example.tmaa_hikeweatherplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
//uvodni obrazovka pro nastaveni profilu
@Composable
fun InitialS(onSetupComplete: (String, String) -> Unit) {
    //stavy pro data z formulare
    var name by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("1") }
    var month by remember { mutableStateOf("1") }
    var year by remember { mutableStateOf("1990") }
    var agreed by remember { mutableStateOf(false) }
    //stavy pro dropdown menu
    var dExpanded by remember { mutableStateOf(false) }
    var mExpanded by remember { mutableStateOf(false) }
    var yExpanded by remember { mutableStateOf(false) }
    //stav pro scroll
    val scrollState = rememberScrollState()
    //hlavni kontejner
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //hlavni nadpis obrazovky
        Text("Nastavení profilu", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        //pole pro jmeno
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Vaše jméno") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Datum narození:", style = MaterialTheme.typography.titleMedium)
        //radek s vyberem dne mesice roku
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            //den pres dropdown
            Box {
                OutlinedButton(onClick = { dExpanded = true }) { Text(day) }
                DropdownMenu(expanded = dExpanded, onDismissRequest = { dExpanded = false }) {
                    (1..31).forEach { d ->
                        DropdownMenuItem(text = { Text(d.toString()) }, onClick = { day = d.toString(); dExpanded = false })
                    }
                }
            }
            //mesic pres dropdown
            Box {
                OutlinedButton(onClick = { mExpanded = true }) { Text(month) }
                DropdownMenu(expanded = mExpanded, onDismissRequest = { mExpanded = false }) {
                    (1..12).forEach { m ->
                        DropdownMenuItem(text = { Text(m.toString()) }, onClick = { month = m.toString(); mExpanded = false })
                    }
                }
            }
            //rok pres dropdown
            Box {
                OutlinedButton(onClick = { yExpanded = true }) { Text(year) }
                DropdownMenu(expanded = yExpanded, onDismissRequest = { yExpanded = false }) {
                    (1950..2025).forEach { y ->
                        DropdownMenuItem(text = { Text(y.toString()) }, onClick = { year = y.toString(); yExpanded = false })
                    }
                }
            }
        }
        //souhlas s podminkama
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Checkbox(checked = agreed, onCheckedChange = { agreed = it })
            Text("Souhlasím s podmínkami užití")
        }
        //dokonceni, jen pri potvrzeni podminek
        Button(
            onClick = { onSetupComplete(name, "$day. $month. $year") },
            enabled = agreed && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Uložit a začít")
        }
    }
}