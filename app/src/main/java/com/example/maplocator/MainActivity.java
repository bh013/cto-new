package com.example.maplocator;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";

    private MapView mapView;
    private EditText etApiEndpoint;
    private Button btnSetStart;
    private Button btnSetDestination;
    private Button btnSubmit;
    private TextView tvStatus;
    private TextView tvStartCoords;
    private TextView tvDestCoords;
    private ProgressBar progressBar;

    private Marker startMarker;
    private Marker destinationMarker;
    private GeoPoint startLocation;
    private GeoPoint destinationLocation;

    private enum SelectionMode {
        NONE,
        START,
        DESTINATION
    }

    private SelectionMode currentMode = SelectionMode.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        
        setContentView(R.layout.activity_main);

        initializeViews();
        requestPermissions();
        setupMap();
        setupListeners();
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        etApiEndpoint = findViewById(R.id.etApiEndpoint);
        btnSetStart = findViewById(R.id.btnSetStart);
        btnSetDestination = findViewById(R.id.btnSetDestination);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvStatus = findViewById(R.id.tvStatus);
        tvStartCoords = findViewById(R.id.tvStartCoords);
        tvDestCoords = findViewById(R.id.tvDestCoords);
        progressBar = findViewById(R.id.progressBar);
    }

    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Some permissions were denied. App may not work correctly.", 
                                 Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(13.0);
        
        GeoPoint startPoint = new GeoPoint(37.7749, -122.4194);
        mapController.setCenter(startPoint);

        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                handleMapTap(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay);
    }

    private void setupListeners() {
        btnSetStart.setOnClickListener(v -> {
            currentMode = SelectionMode.START;
            btnSetStart.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            btnSetDestination.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            updateStatus(getString(R.string.status_select_start));
        });

        btnSetDestination.setOnClickListener(v -> {
            currentMode = SelectionMode.DESTINATION;
            btnSetDestination.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            btnSetStart.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            updateStatus(getString(R.string.status_select_destination));
        });

        btnSubmit.setOnClickListener(v -> submitLocations());
    }

    private void handleMapTap(GeoPoint point) {
        if (currentMode == SelectionMode.START) {
            setStartLocation(point);
        } else if (currentMode == SelectionMode.DESTINATION) {
            setDestinationLocation(point);
        }
    }

    private void setStartLocation(GeoPoint point) {
        startLocation = point;

        if (startMarker != null) {
            mapView.getOverlays().remove(startMarker);
        }

        startMarker = createMarker(point, "Start", getString(R.string.start_label), true);
        mapView.getOverlays().add(startMarker);
        mapView.invalidate();

        updateStartCoordinates();
        updateStatus(getString(R.string.status_start_set));
        resetModeButtons();
        currentMode = SelectionMode.NONE;
    }

    private void setDestinationLocation(GeoPoint point) {
        destinationLocation = point;

        if (destinationMarker != null) {
            mapView.getOverlays().remove(destinationMarker);
        }

        destinationMarker = createMarker(point, "Destination", getString(R.string.dest_label), false);
        mapView.getOverlays().add(destinationMarker);
        mapView.invalidate();

        updateDestinationCoordinates();
        updateStatus(getString(R.string.status_destination_set));
        resetModeButtons();
        currentMode = SelectionMode.NONE;
    }

    private Marker createMarker(GeoPoint point, String title, String snippet, boolean isStart) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setSnippet(snippet);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setDraggable(true);

        if (isStart) {
            marker.setIcon(getMarkerIcon(Color.GREEN));
        } else {
            marker.setIcon(getMarkerIcon(Color.RED));
        }

        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (isStart) {
                    startLocation = marker.getPosition();
                    updateStartCoordinates();
                } else {
                    destinationLocation = marker.getPosition();
                    updateDestinationCoordinates();
                }
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
            }
        });

        return marker;
    }

    private Drawable getMarkerIcon(int color) {
        Drawable icon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default);
        if (icon != null) {
            icon.setTint(color);
        }
        return icon;
    }

    private void updateStartCoordinates() {
        if (startLocation != null) {
            String coords = String.format("Start: %.6f, %.6f", 
                startLocation.getLatitude(), startLocation.getLongitude());
            tvStartCoords.setText(coords);
        }
    }

    private void updateDestinationCoordinates() {
        if (destinationLocation != null) {
            String coords = String.format("Dest: %.6f, %.6f", 
                destinationLocation.getLatitude(), destinationLocation.getLongitude());
            tvDestCoords.setText(coords);
        }
    }

    private void resetModeButtons() {
        btnSetStart.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnSetDestination.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
    }

    private void updateStatus(String message) {
        tvStatus.setText(message);
    }

    private void submitLocations() {
        if (startLocation == null || destinationLocation == null) {
            Toast.makeText(this, R.string.error_both_locations, Toast.LENGTH_SHORT).show();
            return;
        }

        String apiUrl = etApiEndpoint.getText().toString().trim();
        if (apiUrl.isEmpty()) {
            Toast.makeText(this, R.string.error_api_endpoint, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        updateStatus(getString(R.string.status_submitting));

        NetworkHelper.postLocationData(
            apiUrl,
            startLocation.getLatitude(),
            startLocation.getLongitude(),
            destinationLocation.getLatitude(),
            destinationLocation.getLongitude(),
            new NetworkHelper.NetworkCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        updateStatus(getString(R.string.status_success));
                        tvStatus.setTextColor(getResources().getColor(R.color.green));
                        Toast.makeText(MainActivity.this, "Response: " + response, 
                                     Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        updateStatus(getString(R.string.error_network, error));
                        tvStatus.setTextColor(getResources().getColor(R.color.red));
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
