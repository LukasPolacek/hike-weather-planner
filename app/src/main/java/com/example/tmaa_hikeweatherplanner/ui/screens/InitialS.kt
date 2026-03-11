package com.example.tmaa_hikeweatherplanner.ui.screens

import android.Manifest
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tmaa_hikeweatherplanner.R

@Composable
fun InitialS(onSetupComplete: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("1") }
    var month by rememberSaveable { mutableStateOf("1") }
    var year by rememberSaveable { mutableStateOf("1990") }
    var agreed by rememberSaveable { mutableStateOf(false) }

    var dEx by rememberSaveable { mutableStateOf(false) }
    var mEx by rememberSaveable { mutableStateOf(false) }
    var yEx by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.profile_setup_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.your_name), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Zadejte jméno...") }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.birth_date), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    BirthPicker(day, month, year, {day=it}, {month=it}, {year=it}, dEx, mEx, yEx, {dEx=it}, {mEx=it}, {yEx=it})
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.your_name), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(24.dp))

                Text(stringResource(R.string.birth_date), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                BirthPicker(day, month, year, {day=it}, {month=it}, {year=it}, dEx, mEx, yEx, {dEx=it}, {mEx=it}, {yEx=it})
            }
        }

        Spacer(Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Checkbox(checked = agreed, onCheckedChange = { agreed = it })
            Text(stringResource(R.string.agree_terms))
        }
        Button(
            onClick = { onSetupComplete(name, "$day. $month. $year") },
            enabled = agreed && name.isNotBlank(),
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
        ) { Text(stringResource(R.string.save_and_start)) }
    }
}

@Composable
fun BirthPicker(
    d: String, m: String, y: String,
    onD: (String)->Unit, onM: (String)->Unit, onY: (String)->Unit,
    de: Boolean, me: Boolean, ye: Boolean,
    sD: (Boolean)->Unit, sM: (Boolean)->Unit, sY: (Boolean)->Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { sD(true) }, modifier = Modifier.fillMaxWidth()) { Text(d) }
            DropdownMenu(expanded = de, onDismissRequest = { sD(false) }) {
                (1..31).forEach { DropdownMenuItem(text = { Text(it.toString()) }, onClick = { onD(it.toString()); sD(false) }) }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { sM(true) }, modifier = Modifier.fillMaxWidth()) { Text(m) }
            DropdownMenu(expanded = me, onDismissRequest = { sM(false) }) {
                (1..12).forEach { DropdownMenuItem(text = { Text(it.toString()) }, onClick = { onM(it.toString()); sM(false) }) }
            }
        }
        Box(modifier = Modifier.weight(1.2f)) {
            OutlinedButton(onClick = { sY(true) }, modifier = Modifier.fillMaxWidth()) { Text(y) }
            DropdownMenu(expanded = ye, onDismissRequest = { sY(false) }) {
                (1950..2025).forEach { DropdownMenuItem(text = { Text(it.toString()) }, onClick = { onY(it.toString()); sY(false) }) }
            }
        }
    }
}