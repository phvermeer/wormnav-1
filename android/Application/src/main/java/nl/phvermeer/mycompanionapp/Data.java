package nl.phvermeer.mycompanionapp;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import pt.karambola.gpx.beans.Point;
import pt.karambola.gpx.beans.Track;

public final class Data {
    public static Track track = null;
    public static List<Point> waypoints = new ArrayList<>();

    public static List<GeoPoint> geoPointsForDevice = new ArrayList<>();

    // default settings
    public static boolean useDefaultSendElevationData = true;
    public static boolean useDefaultOptimization = true;
    public static int defaultMaxPathWpt = 200;
    public static double defaultMaxPathError = 10d;

}
