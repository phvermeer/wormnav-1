package org.andan.android.connectiq.wormnav

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pt.karambola.gpx.parser.GpxParser
import kotlin.system.exitProcess

enum class ScreenId(@StringRes val screenId: Int) {
    SelectGpx(screenId = R.string.select_gpx),
    SelectTrack(screenId = R.string.select_track),
}

@Composable
fun MainScreen (
    navController: NavHostController = rememberNavController()
){
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen
    val currentScreen = ScreenId.valueOf(
        backStackEntry?.destination?.route ?: ScreenId.SelectGpx.name
    )

    Scaffold(
        topBar = {
            MainAppTopBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
            )
        },
        bottomBar = {
            MainAppBottomBar(
                navController = navController,
            )
        }
    ) {
        innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ScreenId.SelectGpx.name,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
            ) {

            composable(route = ScreenId.SelectGpx.name){
                SelectGpxScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.padding_medium))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppTopBar(
    currentScreen: ScreenId,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(currentScreen.screenId)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,

        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { cancel() }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.exit_button)
                )
            }
        },
    )
}

@Composable
fun MainAppBottomBar(navController: NavHostController){
    val currentContext = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        uri?.let{setUri(uri, currentContext)}
    }
    NavigationBar (
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp)
                ,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if(navController.previousBackStackEntry != null){
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                }else{
                    Spacer(Modifier)
                }
                IconButton(
                    onClick = {
                        launcher.launch(arrayOf("application/octet-stream"))

                    }
                ){
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.next_button)
                    )
                }
            }
        },
    )
}

@Composable
fun SelectGpxScreen(modifier: Modifier = Modifier){
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        for (i in 1..40) {
            // show current folder
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
            ) {
                Text(
                    text = "Body Text",
                    fontSize = 30.sp
                )
            }
        }
    }
}

fun cancel(){
    exitProcess(-1)
}

fun setUri(uri: Uri, context: Context){
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val parser = GpxParser()
        val gpx = parser.parseGpx(inputStream)
        if(gpx.tracks.size > 0){
            val track = gpx.tracks[0]
            SendToDeviceUtility.startDeviceBrowserActivity(context, track)
        }
    }catch(e: Exception){
        Log.e("PeterTest", e.message, e)
    }
}