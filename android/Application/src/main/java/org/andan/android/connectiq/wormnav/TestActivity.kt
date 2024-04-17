package org.andan.android.connectiq.wormnav

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

class TestActivity  : ComponentActivity(){

    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        }
    }

    @Composable
    fun TestScreen(){

    }
}