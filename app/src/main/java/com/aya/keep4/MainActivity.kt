package com.aya.keep4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aya.keep4.ui.Keep4Theme
import com.aya.keep4.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Keep4Theme {
                MainScreen()
            }
        }
    }
}
