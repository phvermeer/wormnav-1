package nl.phvermeer.mycompanionapp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.andan.android.connectiq.wormnav.R
import org.andan.android.connectiq.wormnav.SendToDeviceUtility
import pt.karambola.gpx.beans.Gpx
import pt.karambola.gpx.beans.Point
import pt.karambola.gpx.beans.Track
import pt.karambola.gpx.parser.GpxParser
import pt.karambola.gpx.util.GpxUtils
import kotlin.system.exitProcess

enum class ScreenId(@StringRes val screenId: Int) {
    SelectGpx(screenId = R.string.select_gpx),
    SelectTrack(screenId = R.string.select_track),
}

@Composable
fun MainScreen (
    viewModel: MyViewModel = MyViewModel(),
    navController: NavHostController = rememberNavController(),
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
/*
        bottomBar = {
            MainAppBottomBar(
                navController = navController,
            )
        }
 */
    ) {
        innerPadding ->
        val data by viewModel.data.collectAsState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top
        ) {
            val rowModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                .padding(top = dimensionResource(R.dimen.padding_medium))
            SelectGpxRow(
                modifier = rowModifier,
                onSelected = {gpx ->
                    viewModel.gpx = gpx
                }
            )

            // Gpx Info
            data.gpx?.let { gpx ->
                GpxInfoRow(modifier = rowModifier, gpx = gpx)

                // Show Waypoints
                WaypointsSelectionRow(
                    modifier = rowModifier,
                    waypoints = gpx.points,
                    onSelected = {
                        viewModel.waypoints = it
                    },
                    currentWaypoints = data.waypoints,
                )

                TrackSelectionRow(
                    modifier = rowModifier,
                    tracks = gpx.tracks,
                    currentTrack = data.track,
                    onSelected = { viewModel.track = it },
                )

                // Show Track Selector
                TrackDropDownRow(
                    modifier = rowModifier,
                    tracks = gpx.tracks,
                    currentTrack = data.track,
                    onSelected = { viewModel.track = it }
                )

                data.track?.let{
                    // Show Track Info
                    TrackInfoRow(
                        modifier = rowModifier,
                        track = it
                    )

                    // Show Send Button
                    SendToDeviceRow(
                        modifier = rowModifier,
                        track = it,
                        waypoints = data.waypoints,
                    )
                }
            }
        }
/*
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

 */
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
                    onClick = { /* ToDo */ }
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

fun cancel(){
    exitProcess(-1)
}
/*
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
*/
@Composable
fun SelectGpxRow(modifier: Modifier = Modifier, onSelected: (gpx: Gpx) -> Unit = {}){
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {it->
        it?.let{uri->
            val inputStream = context.contentResolver.openInputStream(uri)
            val parser = GpxParser()
            val gpx = parser.parseGpx(inputStream)
            gpx?.let {
                onSelected(it)
            }
        }
    }
    Row(
        modifier
        //    .background(color = Color.Red)
    ){
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                filePicker.launch(arrayOf("application/octet-stream"))
            }) {
            Text(stringResource(R.string.select_gpx))
        }
    }
}
@Composable
fun GpxInfoRow(modifier: Modifier = Modifier, gpx: Gpx){
    Row(modifier) {
        Text("GPX: ${gpx.tracks.size} tracks, ${gpx.points.size} waypoints")
    }
}

@Composable
fun TrackDropDownRow(modifier: Modifier = Modifier, tracks: List<Track>, onSelected: (track: Track) -> Unit = {}, currentTrack: Track? = null){
    var expanded by remember { mutableStateOf(false) }
    var textFilledSize by remember { mutableStateOf( Size.Zero) }
    val icon = if(expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
    var selectedTrack by remember { mutableStateOf<Track?>( currentTrack ) }
    currentTrack?.let{ selectedTrack = it }

    // show the dropbox
    Row(modifier){
        OutlinedTextField(
            value = selectedTrack?.name ?: "",
            onValueChange = { /* do nothing */ },
            modifier = Modifier
                .fillMaxWidth()
                .onPlaced { coordinates -> textFilledSize = coordinates.size.toSize() },
            label = { Text("Select Item") },
            trailingIcon = { Icon(icon, "", Modifier.clickable { expanded = !expanded }) },
            readOnly = true,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { textFilledSize.width.toDp() })

        ) {
            tracks.forEach { track ->
                DropdownMenuItem(
                    text = { Text(track.name) },
                    onClick = {
                        onSelected(track)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TrackInfoRow(modifier: Modifier = Modifier, track: Track){
    val distance = GpxUtils.lengthOfTrack(track)
    Row(modifier){
        Text("${String.format("%.3f", distance / 1000)}km")
    }
}

@Composable
fun WaypointsSelectionRow(modifier: Modifier = Modifier, waypoints: Collection<Point>, onSelected: (waypoints: MutableList<Point>) -> Unit = {}, currentWaypoints: Collection<Point>? = null){
    var selectedWaypoints by remember { mutableStateOf(currentWaypoints?.toMutableList() ?: mutableListOf()) }
    currentWaypoints?.let{ selectedWaypoints = currentWaypoints.toMutableList() }

    Row(
        modifier = modifier,
    ){
        Text(
            text = stringResource(R.string.waypoints),
            style = MaterialTheme.typography.headlineMedium,
        )
    }

    waypoints.forEach {waypoint->
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ){
            Checkbox(checked = selectedWaypoints.contains(waypoint), onCheckedChange = {
                if(it){
                    selectedWaypoints.add(waypoint)
                }else{
                    selectedWaypoints.remove(waypoint)
                }
                onSelected(selectedWaypoints)
            } )
            Text(
                text = waypoint.name,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
fun SendToDeviceRow(modifier: Modifier = Modifier, track: Track, waypoints: List<Point>){
    val context = LocalContext.current
    Row(modifier){
        Button(
            onClick = {
                SendToDeviceUtility.startDeviceBrowserActivity(context, waypoints, track)
            }
        ){
            Text(stringResource(R.string.send_to_device))
        }
    }
}

@Composable
fun TrackSelectionRow(modifier: Modifier = Modifier, tracks: Collection<Track>, onSelected: (track: Track) -> Unit = {}, currentTrack: Track? = null){
    var selectedTrack by remember { mutableStateOf<Track?>( currentTrack ) }
    currentTrack?.let{ selectedTrack = it }

    Row(modifier){
        Text(
            text = stringResource(R.string.tracks),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
    tracks.forEach {track ->
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ){
            RadioButton(
                selected = selectedTrack?.equals(track) ?: false,
                onClick = { onSelected(track) },
            )
            Text(
                text = track.name,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}