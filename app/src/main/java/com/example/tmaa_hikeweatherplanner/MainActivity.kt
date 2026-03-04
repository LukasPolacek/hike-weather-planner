package com.example.tmaa_hikeweatherplanner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.tmaa_hikeweatherplanner.ui.navigation.HikeNavigation
import com.example.tmaa_hikeweatherplanner.ui.theme.TMAAHikeWeatherPlannerTheme

class MainActivity : ComponentActivity() {
    //volane pri prvnim vytvoreni aktivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //zapis do logu pro sledovani zivotniho cyklu
        Log.i("Lifecycle", "onCreate")
        enableEdgeToEdge()
        setContent {
            TMAAHikeWeatherPlannerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HikeNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    //volano kdyz je aktivita viditelna uzivateli
    override fun onStart() { super.onStart(); Log.i("Lifecycle", "onStart") }
    //volano kdyz uzivatel zacina s aplikaci pracovat
    override fun onResume() { super.onResume(); Log.i("Lifecycle", "onResume") }
    //volano pri prekryti aktivity jinym oknem
    override fun onPause() { super.onPause(); Log.i("Lifecycle", "onPause") }
    //volano kdyz aktivita jiz neni viditelna
    override fun onStop() { super.onStop(); Log.i("Lifecycle", "onStop") }
    //volano pred uplnym znicenim aktivity z pameti
    override fun onDestroy() { super.onDestroy(); Log.i("Lifecycle", "onDestroy") }
}