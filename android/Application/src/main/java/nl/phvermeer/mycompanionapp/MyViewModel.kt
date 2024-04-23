package nl.phvermeer.mycompanionapp

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pt.karambola.gpx.beans.Gpx
import pt.karambola.gpx.beans.Point
import pt.karambola.gpx.beans.Track

data class MyData(
    val gpx: Gpx? = null,
    val track: Track? = null,
    val waypoints: List<Point> = listOf<Point>(),
    val error: Exception? = null,
){
}

class MyViewModel: ViewModel() {
    private val _data = MutableStateFlow(MyData())
    val data: StateFlow<MyData> = _data.asStateFlow()

    var gpx: Gpx?
        get() = _data.value.gpx
        set(it) = _data.update { currentState ->
            // update track for new gpx (when only 1 track in gpx, use this track without asking)
            var track = currentState.track
            it?.let{
                if(it.tracks.size == 1){
                    track = it.tracks[0]
                }else{
                    track = null
                }
            }

            // update waypoints for new gpx (select all waypoints for new gpx)
            val waypoints = it?.points?.toMutableList() ?: mutableListOf<Point>()

            currentState.copy(
                gpx = it,
                track = track,
                waypoints = waypoints,
            )
        }

    var track: Track?
        get() = _data.value.track
        set(it) = _data.update { currentState ->
            currentState.copy(
                track = it,
            )
        }

    var waypoints: Collection<Point>
        get() = _data.value.waypoints
        set(it) = _data.update { currentState ->
            // deep compare to force update
            val wps = _data.value.waypoints
            if (wps.containsAll(it) && it.containsAll(wps))
                currentState
            else
                currentState.copy(
                    // clone to force update
                    waypoints = it.toList(),
                )
        }

    var error: Exception?
        get() = _data.value.error
        set(it) = _data.update { currentState ->
            currentState.copy(
                error = it,
            )
        }
}