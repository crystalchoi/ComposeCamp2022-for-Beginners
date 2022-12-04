/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.cupcake

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cupcake.data.DataSource.flavors
import com.example.cupcake.data.DataSource.quantityOptions
import com.example.cupcake.ui.OrderSummaryScreen
import com.example.cupcake.ui.OrderViewModel
import com.example.cupcake.ui.SelectOptionScreen
import com.example.cupcake.ui.StartOrderScreen
import com.example.cupcake.ui.theme.CupcakeTheme
private const val TAG = "CupcakeScreen"

enum class CupcakeScreen {
    Start
    , Flavor
    , Pickup
    , Summary
}

/**
 * Composable that displays the topBar and displays back button if back navigation is possible.
 */
@Composable
fun CupcakeAppBar(
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(id = R.string.app_name)) },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        }
    )
}

@Composable
fun CupcakeApp(modifier: Modifier = Modifier, viewModel: OrderViewModel = viewModel()){
    // TODO: Create NavController
    val navController = rememberNavController()
            
    // TODO: Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()

    // TODO: Get the name of the current screen
    val currentScreen = backStackEntry?.destination?.route ?: CupcakeScreen.Start.name

    Scaffold(
        topBar = {
            CupcakeAppBar(
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()

        // TODO: add NavHost
        NavHost(navController = navController
            , startDestination = CupcakeScreen.Start.name
            , modifier = modifier.padding(innerPadding)) {
            composable(route = CupcakeScreen.Start.name) {
                StartOrderScreen(quantityOptions = quantityOptions
                    , onNextButtonClicked = {
                        viewModel.setQuantity(it)
                        navController.navigate(CupcakeScreen.Flavor.name)
                    }
                )
            }
            composable(route = CupcakeScreen.Flavor.name) {
                val context = LocalContext.current
                SelectOptionScreen(subtotal = uiState.price
                    , options = flavors.map {id -> stringResource(id) }
                    , onSelectionChanged = { viewModel.setFlavor(it) }
                    , onCancelButtonClicked = { cancelOrderAndNavigateToStart(viewModel, navController) }
                    , onNextButtonClicked = { navController.navigate(CupcakeScreen.Pickup.name) }
                )
            }
            composable(route = CupcakeScreen.Pickup.name) {
                SelectOptionScreen(subtotal = uiState.price
                    , options = uiState.pickupOptions
                    , onSelectionChanged = { viewModel.setDate(it) }
                    , onCancelButtonClicked = { cancelOrderAndNavigateToStart(viewModel, navController) }
                    , onNextButtonClicked = { navController.navigate(CupcakeScreen.Summary.name)}
                )
            }
            composable(route = CupcakeScreen.Summary.name) {
                val context = LocalContext.current
                OrderSummaryScreen(
                    orderUiState = uiState
                    , onCancelButtonClicked = { cancelOrderAndNavigateToStart(viewModel, navController) }
                    , onSendButtonClicked = { subject: String, summary: String ->
                        shareOrder(context = context, subject = subject, summary = summary)
                    }
                )
            }
        }
    }
}

private fun cancelOrderAndNavigateToStart(viewModel: OrderViewModel
    , navController: NavHostController
) {
    Log.d(TAG, "onCancelButtonClicked")
    viewModel.resetOrder()
    navController.popBackStack(CupcakeScreen.Start.name, inclusive = false)
}


private fun shareOrder(context: Context
    , subject: String
    , summary: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(Intent.createChooser(intent
                                    , context.getString(R.string.new_cupcake_order))
    )


}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CupcakeAppPreview(){
    CupcakeTheme {
        CupcakeApp()
    }
}