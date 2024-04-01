package org.andan.android.connectiq.wormnav;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import pt.karambola.gpx.beans.RoutePoint;
import pt.karambola.gpx.util.GpxRouteUtils;

import static org.andan.android.connectiq.wormnav.R.id.osmmap;

/**
 * Route Creator activity created by piotr on 02.05.17.
 */
public class RouteOptimizerActivity extends Utils {

    private final String TAG = RouteOptimizerActivity.class.getName();

    private final double MAX_ZOOM_LEVEL = 19;
    private final double MIN_ZOOM_LEVEL = 4;

    Button fitButton;
    Button zoomInButton;
    Button zoomOutButton;
    Button saveButton;

    TextView routePrompt;

    NumberPicker maxPointsPicker;

    GpxRouteUtils routeUtils;

    private MapView mMapView;
    private IMapController mapController;

    private MyLocationNewOverlay mLocationOverlay;

    private RotationGestureOverlay mRotationGestureOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#00ffffff")));
            actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#00ffffff")));
        }

        Configuration.getInstance().setUserAgentValue(this.getPackageName());

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_route_optimizer);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        createLocationRequest();

        if (Data.sCardinalGeoPoints == null) {
            Data.sCardinalGeoPoints = new ArrayList<>();
        }

        Data.sSourceRoutePointsNumber = Data.sCopiedRoute.getRoutePoints().size();

        setUpMap();
        refreshMap();
    }

    private void setUpMap() {

        mMapView = (MapView) findViewById(osmmap);

        mMapView.setTilesScaledToDpi(true);

        mMapView.setTileSource(TileSourceFactory.MAPNIK);

        TilesOverlay tilesOverlay = mMapView.getOverlayManager().getTilesOverlay();
        //tilesOverlay.setOvershootTileCache(tilesOverlay.getOvershootTileCache() * 2);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMapView);
        mLocationOverlay.enableMyLocation();

        mRotationGestureOverlay = new RotationGestureOverlay(mMapView);
        mRotationGestureOverlay.setEnabled(true);

        mMapView.setMaxZoomLevel(MAX_ZOOM_LEVEL);
        mMapView.setMinZoomLevel(MIN_ZOOM_LEVEL);

        mMapView.setMultiTouchControls(true);

        mapController = mMapView.getController();

        routeUtils = new GpxRouteUtils(Data.sCopiedRoute);

        restoreMapPosition();

        setUpButtons();
        setButtonsState();

        simplifyRoute(Data.sSourceRoutePointsNumber);
        /*
         * The route points limitation will actually be performed by the method called above
         * but we need to display the Toast here
         */
        if (Data.OPTIMIZER_POINTS_LIMIT <= Data.sSourceRoutePointsNumber) {
            Toast.makeText(getApplicationContext(), String.format(getResources().getString(R.string.route_points_limited),
                    Data.OPTIMIZER_POINTS_LIMIT), Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreMapPosition() {

        if (Data.sLastZoom == null) {
            mapController.setZoom(3);
        } else {
            mapController.setZoom(Data.sLastZoom);
        }

        if (Data.sLastCenter == null) {
            mapController.setCenter(new GeoPoint(0d, 0d));
        } else {
            mapController.setCenter(Data.sLastCenter);
        }
    }

    private void refreshMap() {

        mMapView.getOverlays().clear();

        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mMapView.getOverlays().add(mScaleBarOverlay);

        mMapView.getOverlays().add(mLocationOverlay);

        if (Data.sAllowRotation) {
            mMapView.getOverlays().add(this.mRotationGestureOverlay);
        }

        mScaleBarOverlay.setScaleBarOffset(
                (int) (getResources().getDisplayMetrics().widthPixels / 2 - getResources()
                        .getDisplayMetrics().xdpi / 2), 10);

        Polyline routeOverlay = new Polyline();
        routeOverlay.setColor(Color.parseColor("#0066ff"));

        Data.routeNodes = new ArrayList<>();

        List<RoutePoint> allRoutePointsList = Data.sCopiedRoute.getRoutePoints();

        for (RoutePoint routePoint : allRoutePointsList) {
            GeoPoint node = new GeoPoint(routePoint.getLatitude(), routePoint.getLongitude());
            Data.routeNodes.add(node);
        }
        routeOverlay.setPoints(Data.routeNodes);
        mMapView.getOverlays().add(routeOverlay);

        /*
         * In contrary to the RouteEditorActivity, we need to see all route points at a time.
         * Let's use the same simple bitmap for all of them.
         *
         * Sorry about iterating this twice:
         * markers look better when drawn above the polyline
         */
        for (RoutePoint routePoint : allRoutePointsList) {

            GeoPoint markerPosition = new GeoPoint(routePoint.getLatitude(), routePoint.getLongitude());

            Marker marker = new Marker(mMapView);
            marker.setPosition(markerPosition);
            marker.setAnchor(0.5f, 0.5f);
            marker.setDraggable(false);
            marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.swp_normal, null));

            mMapView.getOverlays().add(marker);
        }

        mMapView.invalidate();
        setButtonsState();
    }

    private void setUpButtons() {

        locationButton = (Button) findViewById(R.id.location_button);
        locationButton.setEnabled(false);
        locationButton.getBackground().setAlpha(0);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.setZoom(18);
                mapController.setCenter(Data.sCurrentPosition);
                setButtonsState();
            }
        });

        fitButton = (Button) findViewById(R.id.fit_button);
        fitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Data.routeNodes != null && Data.routeNodes.size() > 1) {
                    mMapView.zoomToBoundingBox(findBoundingBox(Data.routeNodes), false);
                }
                setButtonsState();
            }
        });
        zoomInButton = (Button) findViewById(R.id.zoom_in_button);
        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mapController.setZoom(mMapView.getProjection().getZoomLevel() + 1);
                setButtonsState();
            }
        });
        zoomOutButton = (Button) findViewById(R.id.zoom_out_button);
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mapController.setZoom(mMapView.getProjection().getZoomLevel() - 1);
                setButtonsState();
            }
        });

        saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(RouteOptimizerActivity.this, RoutesBrowserActivity.class);

                if (Data.sSelectedRouteIdx != null) {

                    Data.sRoutesGpx.removeRoute(Data.sFilteredRoutes.get(Data.sSelectedRouteIdx));
                    Data.sRoutesGpx.addRoute(Data.sCopiedRoute);
                    Data.sSelectedRouteIdx = Data.sRoutesGpx.getRoutes().indexOf(Data.sCopiedRoute);

                } else {

                    Data.sRoutesGpx.addRoute(Data.sCopiedRoute);
                    Data.sSelectedRouteIdx = Data.sRoutesGpx.getRoutes().indexOf(Data.sCopiedRoute);
                    setResult(Data.NEW_ROUTE_ADDED, i);
                }
                finish();
            }
        });

        routePrompt = (TextView) findViewById(R.id.route_prompt);

        maxPointsPicker = (NumberPicker) findViewById(R.id.maxPointsPicker);
        maxPointsPicker.setEnabled(false);
        maxPointsPicker.setAlpha(0);

        final TextView copyright = (TextView) findViewById(R.id.copyright);
        copyright.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setButtonsState() {

        if (mMapView.getProjection().getZoomLevel() < MAX_ZOOM_LEVEL) {
            zoomInButton.setEnabled(true);
            zoomInButton.getBackground().setAlpha(255);
        } else {
            zoomInButton.setEnabled(false);
            zoomInButton.getBackground().setAlpha(100);
        }

        if (mMapView.getProjection().getZoomLevel() > MIN_ZOOM_LEVEL) {
            zoomOutButton.setEnabled(true);
            zoomOutButton.getBackground().setAlpha(255);
        } else {
            zoomOutButton.setEnabled(false);
            zoomOutButton.getBackground().setAlpha(100);
        }

        if (Data.sCopiedRoute.isChanged()) {
            saveButton.setEnabled(true);
            saveButton.getBackground().setAlpha(255);
        } else {
            saveButton.setEnabled(false);
            saveButton.getBackground().setAlpha(100);
        }

    }

    public void onResume() {
        super.onResume();

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        startLocationUpdates();
        restoreMapPosition();
    }

    @Override
    protected void onPause() {

        super.onPause();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        Data.sLastZoom = mMapView.getZoomLevelDouble();
        Data.sLastCenter = new GeoPoint(mMapView.getMapCenter().getLatitude(), mMapView.getMapCenter().getLongitude());
        Data.sLastRotation = mMapView.getRotation();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*
         * Handle the back button
         */
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            /*
             * If data changed
             */
            if (Data.sCopiedRoute.isChanged() && Data.sCopiedRoute.getRoutePoints().size() > 1) {
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.map_warning)
                        .setTitle(R.string.dialog_save_changes_title)
                        .setMessage(R.string.dialog_route_changed_message)

                        .setPositiveButton(R.string.dialog_apply, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (!Data.sCopiedRoute.getRoutePoints().isEmpty()) {

                                    Data.sRoutesGpx.removeRoute(Data.sFilteredRoutes.get(Data.sSelectedRouteIdx));
                                    Data.sRoutesGpx.addRoute(Data.sCopiedRoute);
                                    Data.sSelectedRouteIdx = Data.sRoutesGpx.getRoutes().indexOf(Data.sCopiedRoute);

                                }
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.dialog_discard, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                finish();
                            }

                        })
                        .show();
                return true;

            } else {

                finish();
                return true;
            }

        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void simplifyRoute(final int source_route_points_number) {

        final String promptFormat = getResources().getString(R.string.optimizer_prompt);

        maxPointsPicker.setEnabled(true);
        maxPointsPicker.setAlpha(0.8f);

        maxPointsPicker.setMinValue(5);

        if (source_route_points_number <= Data.OPTIMIZER_POINTS_LIMIT) {

            maxPointsPicker.setMaxValue(source_route_points_number);
            maxPointsPicker.setValue(source_route_points_number);

            Data.sCurrentMaxPointsNumber = source_route_points_number;

        } else {

            maxPointsPicker.setMaxValue(Data.OPTIMIZER_POINTS_LIMIT);
            maxPointsPicker.setValue(Data.OPTIMIZER_POINTS_LIMIT);

            Data.sCurrentMaxPointsNumber = Data.OPTIMIZER_POINTS_LIMIT;
        }

        maxPointsPicker.setWrapSelectorWheel(true);

        maxPointsPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {

            @Override
            public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {

                Data.sCurrentMaxPointsNumber = newVal;

                double simplificationError = routeUtils.simplify(Data.sCurrentMaxPointsNumber, Data.currentMaxErrorMtr);

                refreshMap();

                String promptMessage = String.format(promptFormat, Data.sCurrentMaxPointsNumber, (int) simplificationError);
                routePrompt.setText(promptMessage);
            }
        });

        /*
         * The code below will be executed only once. Each real modification is being done by the
         * onValueChange method above. Let's reset the isChanged boolean, to avoid being asked
         * if to save data on exit when we didn't use the picker.
         */
        double simplificationError = routeUtils.simplify(Data.sCurrentMaxPointsNumber, Data.currentMaxErrorMtr);
        Data.sCopiedRoute.resetIsChanged();
        String promptMessage = String.format(promptFormat, source_route_points_number, (int) simplificationError);
        routePrompt.setText(promptMessage);
        refreshMap();
    }
}