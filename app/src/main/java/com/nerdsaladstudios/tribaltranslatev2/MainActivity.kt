package com.nerdsaladstudios.tribaltranslatev2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nerdsaladstudios.tribaltranslatev2.data.download.ModelDownloadWorker
import com.nerdsaladstudios.tribaltranslatev2.ui.home.HomeScreen
import com.nerdsaladstudios.tribaltranslatev2.ui.setup.ModelSetupScreen
import com.nerdsaladstudios.tribaltranslatev2.ui.theme.TribalTranslateV2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TribalTranslateV2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isSetupComplete by remember {
                        mutableStateOf(ModelDownloadWorker.areAllModelsDownloaded(this))
                    }

                    if (isSetupComplete) {
                        HomeScreen()
                    } else {
                        ModelSetupScreen(
                            onSetupComplete = {
                                isSetupComplete = true
                            }
                        )
                    }
                }
            }
        }
    }
}
