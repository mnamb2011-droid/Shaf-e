package com.shafii.encyclopedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shafii.encyclopedia.ui.navigation.ShafiiRootScaffold
import com.shafii.encyclopedia.ui.theme.ShafiiEncyclopediaTheme
import com.shafii.encyclopedia.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme by viewModel.darkTheme.collectAsState()
            val readerConfig by viewModel.readerConfig.collectAsState()
            ShafiiEncyclopediaTheme(
                darkTheme = darkTheme,
                readerConfig = readerConfig
            ) {
                ShafiiRootScaffold(viewModel = viewModel)
            }
        }
    }
}
