package com.thalyspenha.pipoca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thalyspenha.pipoca.presentation.navigation.PipocaApp
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PipocaTheme {
                PipocaApp()
            }
        }
    }
}
