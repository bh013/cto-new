package com.example.maplocator;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final long DRIVER_POLL_INTERVAL_SECONDS = 8;

    private MapView mapView;

    private LinearLayout selectionPanel;
    private LinearLayout trackingPanel;

    private EditText etApiEndpoint;
    private Button btnSetStart;
    private Button btnSetDestination;
    private Button btnSubmit;

    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvStartCoords;
    private TextView tvDestCoords;

    private TextView tvDriverName;
    private TextView tvVehicle;
    private TextView tvDistance;
    private TextView tvEta;
    private TextView tvLiveStatus;
    private TextView tvPollingStatus;
    private Button btnCancelBooking;

    private Marker startMarker;
    private Marker destinationMarker;
    private Marker driverMarker;

    private GeoPoint startLocation;
    private GeoPoint destinationLocation;

    private enum SelectionMode {
        NONE,
        START,
        DESTINATION
    }

    private enum BookingState {
        LOCATION_SELECTION,
        WAITING_PRICE_CONFIRMATION,
        TRACKING
    }

    private SelectionMode currentMode = SelectionMode.NONE;
    private BookingState bookingState = BookingState.LOCATION_SELECTION;

    private final BookingData bookingData = new BookingData();

    private ScheduledExecutorService pollingExecutor;
    private ScheduledFuture<?> pollingFuture;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int consecutivePollErrors = 0;

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

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }

        updateUiForState();
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);

        selectionPanel = findViewById(R.id.selectionPanel);
        trackingPanel = findViewById(R.id.trackingPanel);

        etApiEndpoint = findViewById(R.id.etApiEndpoint);
        btnSetStart = findViewById(R.id.btnSetStart);
        btnSetDestination = findViewById(R.id.btnSetDestination);
        btnSubmit = findViewById(R.id.btnSubmit);

        tvStatus = findViewById(R.id.tvStatus);
        tvStartCoords = findViewById(R.id.tvStartCoords);
        tvDestCoords = findViewById(R.id.tvDestCoords);
        progressBar = findViewById(R.id.progressBar);

        tvDriverName = findViewById(R.id.tvDriverName);
        tvVehicle = findViewById(R.id.tvVehicle);
        tvDistance = findViewById(R.id.tvDistance);
        tvEta = findViewById(R.id.tvEta);
        tvLiveStatus = findViewById(R.id.tvLiveStatus);
        tvPollingStatus = findViewById(R.id.tvPollingStatus);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
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
                    Toast.makeText(this, getString(R.string.permission_denied_warning),
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

        org.osmdroid.events.MapEventsReceiver mapEventsReceiver = new org.osmdroid.events.MapEventsReceiver() {
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
            if (bookingState != BookingState.LOCATION_SELECTION) {
                return;
            }

            currentMode = SelectionMode.START;
            btnSetStart.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            btnSetDestination.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            updateStatus(getString(R.string.status_select_start));
        });

        btnSetDestination.setOnClickListener(v -> {
            if (bookingState != BookingState.LOCATION_SELECTION) {
                return;
            }

            currentMode = SelectionMode.DESTINATION;
            btnSetDestination.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            btnSetStart.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            updateStatus(getString(R.string.status_select_destination));
        });

        btnSubmit.setOnClickListener(v -> submitLocations());

        btnCancelBooking.setOnClickListener(v -> confirmCancelBooking());
    }

    private void handleMapTap(GeoPoint point) {
        if (bookingState != BookingState.LOCATION_SELECTION) {
            return;
        }

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

        startMarker = createLocationMarker(point, "Start", getString(R.string.start_label), true);
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

        destinationMarker = createLocationMarker(point, "Destination", getString(R.string.dest_label), false);
        mapView.getOverlays().add(destinationMarker);
        mapView.invalidate();

        updateDestinationCoordinates();
        updateStatus(getString(R.string.status_destination_set));
        resetModeButtons();
        currentMode = SelectionMode.NONE;
    }

    private Marker createLocationMarker(GeoPoint point, String title, String snippet, boolean isStart) {
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

    private Marker createDriverMarker(GeoPoint point) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(getString(R.string.driver_label));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setDraggable(false);
        marker.setIcon(getMarkerIcon(Color.BLUE));
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
        tvStatus.setTextColor(getResources().getColor(R.color.black));
    }

    private void submitLocations() {
        if (bookingState != BookingState.LOCATION_SELECTION) {
            return;
        }

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
        btnSetStart.setEnabled(false);
        btnSetDestination.setEnabled(false);
        etApiEndpoint.setEnabled(false);
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
                        btnSetStart.setEnabled(true);
                        btnSetDestination.setEnabled(true);
                        etApiEndpoint.setEnabled(true);

                        BookingQuote quote = parseBookingQuote(response);
                        if (quote == null) {
                            updateStatus(getString(R.string.error_invalid_price_response));
                            tvStatus.setTextColor(getResources().getColor(R.color.red));
                            Toast.makeText(MainActivity.this, response, Toast.LENGTH_LONG).show();
                            return;
                        }

                        bookingData.setRequestId(quote.requestId);
                        bookingData.setPrice(quote.price);

                        showPriceConfirmationDialog(quote);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        btnSetStart.setEnabled(true);
                        btnSetDestination.setEnabled(true);
                        etApiEndpoint.setEnabled(true);
                        updateStatus(getString(R.string.error_network, error));
                        tvStatus.setTextColor(getResources().getColor(R.color.red));
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }

    private static class BookingQuote {
        final double price;
        final String requestId;

        BookingQuote(double price, String requestId) {
            this.price = price;
            this.requestId = requestId;
        }
    }

    private BookingQuote parseBookingQuote(String response) {
        try {
            JSONObject json = new JSONObject(response);

            double price = getFirstDouble(json, "price", "taxiPrice", "amount", "fare");
            String requestId = getFirstString(json, "requestId", "bookingId", "id");

            if (Double.isNaN(price)) {
                return null;
            }

            return new BookingQuote(price, requestId);

        } catch (Exception e) {
            return null;
        }
    }

    private void showPriceConfirmationDialog(BookingQuote quote) {
        String startText = startLocation != null
            ? String.format("%.6f, %.6f", startLocation.getLatitude(), startLocation.getLongitude())
            : getString(R.string.not_available);
        String destText = destinationLocation != null
            ? String.format("%.6f, %.6f", destinationLocation.getLatitude(), destinationLocation.getLongitude())
            : getString(R.string.not_available);

        String message = getString(
            R.string.price_confirm_message,
            formatPrice(quote.price),
            startText,
            destText
        );

        if (quote.requestId != null && !quote.requestId.trim().isEmpty()) {
            message += "\n" + getString(R.string.request_id_line, quote.requestId);
        }

        bookingState = BookingState.WAITING_PRICE_CONFIRMATION;

        new AlertDialog.Builder(this)
            .setTitle(R.string.price_dialog_title)
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel, (dialog, which) -> {
                bookingState = BookingState.LOCATION_SELECTION;
                updateUiForState();
                updateStatus(getString(R.string.status_ready));
            })
            .setPositiveButton(R.string.accept, (dialog, which) -> confirmPrice())
            .show();
    }

    private void confirmPrice() {
        String apiUrl = etApiEndpoint.getText().toString().trim();
        if (apiUrl.isEmpty()) {
            Toast.makeText(this, R.string.error_api_endpoint, Toast.LENGTH_SHORT).show();
            bookingState = BookingState.LOCATION_SELECTION;
            updateUiForState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        btnSetStart.setEnabled(false);
        btnSetDestination.setEnabled(false);
        etApiEndpoint.setEnabled(false);

        updateStatus(getString(R.string.status_confirming_price));

        NetworkHelper.postPriceConfirmation(apiUrl, bookingData.getRequestId(), new NetworkHelper.NetworkCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    String bookingId = parseBookingId(response);
                    if (bookingId == null || bookingId.trim().isEmpty()) {
                        bookingId = bookingData.getRequestId();
                    }
                    bookingData.setBookingId(bookingId);

                    bookingState = BookingState.TRACKING;
                    updateUiForState();
                    updateStatus(getString(R.string.status_tracking));

                    tvLiveStatus.setText(getString(R.string.status_searching_driver));
                    startPollingDriverLocation(true);

                    Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    bookingState = BookingState.LOCATION_SELECTION;
                    updateUiForState();

                    updateStatus(getString(R.string.error_network, error));
                    tvStatus.setTextColor(getResources().getColor(R.color.red));
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String parseBookingId(String response) {
        try {
            JSONObject json = new JSONObject(response);
            return getFirstString(json, "bookingId", "requestId", "id");
        } catch (Exception e) {
            return null;
        }
    }

    private void confirmCancelBooking() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.cancel_booking)
            .setMessage(R.string.cancel_booking_confirm_message)
            .setNegativeButton(R.string.cancel, (d, w) -> {
            })
            .setPositiveButton(R.string.cancel_booking, (d, w) -> cancelBooking())
            .show();
    }

    private void cancelBooking() {
        stopPollingDriverLocation();

        String apiUrl = etApiEndpoint.getText().toString().trim();
        String id = getTrackingId();

        if (!apiUrl.isEmpty() && id != null && !id.trim().isEmpty()) {
            NetworkHelper.cancelBooking(apiUrl, id, new NetworkHelper.NetworkCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show());
                }
            });
        }

        resetBooking();
        updateStatus(getString(R.string.status_ready));
    }

    private void resetBooking() {
        bookingState = BookingState.LOCATION_SELECTION;
        bookingData.setBookingId(null);
        bookingData.setRequestId(null);

        if (driverMarker != null) {
            mapView.getOverlays().remove(driverMarker);
            driverMarker = null;
        }

        consecutivePollErrors = 0;
        updateTrackingUi(null);
        updateUiForState();
    }

    private void updateUiForState() {
        if (bookingState == BookingState.TRACKING) {
            selectionPanel.setVisibility(View.GONE);
            trackingPanel.setVisibility(View.VISIBLE);
        } else {
            selectionPanel.setVisibility(View.VISIBLE);
            trackingPanel.setVisibility(View.GONE);
        }

        if (bookingState != BookingState.TRACKING) {
            btnSubmit.setEnabled(true);
            btnSetStart.setEnabled(true);
            btnSetDestination.setEnabled(true);
            etApiEndpoint.setEnabled(true);
        }
    }

    private void startPollingDriverLocation(boolean immediate) {
        stopPollingDriverLocation();

        String apiUrl = etApiEndpoint.getText().toString().trim();
        String id = getTrackingId();
        if (apiUrl.isEmpty() || id == null || id.trim().isEmpty()) {
            tvPollingStatus.setTextColor(getResources().getColor(R.color.red));
            tvPollingStatus.setText(getString(R.string.error_api_endpoint));
            return;
        }

        tvPollingStatus.setTextColor(getResources().getColor(R.color.green));
        tvPollingStatus.setText(getString(R.string.polling_active));

        pollingExecutor = Executors.newSingleThreadScheduledExecutor();
        long initialDelay = immediate ? 0 : DRIVER_POLL_INTERVAL_SECONDS;

        pollingFuture = pollingExecutor.scheduleAtFixedRate(() -> {
            if (bookingState != BookingState.TRACKING) {
                return;
            }

            NetworkHelper.pollDriverPosition(apiUrl, id, new NetworkHelper.NetworkCallback() {
                @Override
                public void onSuccess(String response) {
                    consecutivePollErrors = 0;
                    handleDriverPollSuccess(response);
                }

                @Override
                public void onError(String error) {
                    consecutivePollErrors++;
                    handleDriverPollError(error);
                }
            });

        }, initialDelay, DRIVER_POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void stopPollingDriverLocation() {
        if (pollingFuture != null) {
            pollingFuture.cancel(true);
            pollingFuture = null;
        }

        if (pollingExecutor != null) {
            pollingExecutor.shutdownNow();
            pollingExecutor = null;
        }

        mainHandler.post(() -> {
            if (tvPollingStatus != null) {
                tvPollingStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                tvPollingStatus.setText(getString(R.string.polling_stopped));
            }
        });
    }

    private void handleDriverPollSuccess(String response) {
        DriverUpdate update = parseDriverUpdate(response);
        if (update == null) {
            handleDriverPollError(getString(R.string.error_invalid_driver_location));
            return;
        }

        bookingData.setDriverLat(update.driverLat);
        bookingData.setDriverLng(update.driverLng);
        bookingData.setDriverName(update.driverName);
        bookingData.setDriverVehicle(update.vehicle);
        bookingData.setEta(update.eta);
        bookingData.setStatus(update.status);

        mainHandler.post(() -> {
            GeoPoint point = new GeoPoint(update.driverLat, update.driverLng);
            updateDriverMarker(point);
            updateTrackingUi(update);
            autoFitMarkersIfNeeded(point);

            if (update.status != null) {
                String statusLower = update.status.toLowerCase();
                if (statusLower.contains("arrived") || statusLower.contains("completed") || statusLower.contains("cancelled")) {
                    stopPollingDriverLocation();
                }
            }
        });
    }

    private void handleDriverPollError(String error) {
        mainHandler.post(() -> {
            tvPollingStatus.setTextColor(getResources().getColor(R.color.red));
            tvPollingStatus.setText(getString(R.string.polling_error, consecutivePollErrors));

            if (consecutivePollErrors == 1 || consecutivePollErrors % 3 == 0) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class DriverUpdate {
        final double driverLat;
        final double driverLng;
        final String driverName;
        final String vehicle;
        final String eta;
        final String status;

        DriverUpdate(double driverLat, double driverLng, String driverName, String vehicle, String eta, String status) {
            this.driverLat = driverLat;
            this.driverLng = driverLng;
            this.driverName = driverName;
            this.vehicle = vehicle;
            this.eta = eta;
            this.status = status;
        }
    }

    private DriverUpdate parseDriverUpdate(String response) {
        try {
            JSONObject json = new JSONObject(response);

            double lat = getFirstDouble(json, "driverLat", "lat", "latitude");
            double lng = getFirstDouble(json, "driverLng", "lng", "lon", "longitude");
            if (Double.isNaN(lat) || Double.isNaN(lng)) {
                return null;
            }

            String name = getFirstString(json, "driverName", "name", "driverId");
            String vehicle = getFirstString(json, "vehicle", "car", "vehicleInfo");
            String eta = getFirstString(json, "eta", "etaMinutes", "etaMins", "etaText");
            String status = getFirstString(json, "status", "message", "state");

            return new DriverUpdate(lat, lng, name, vehicle, eta, status);

        } catch (Exception e) {
            return null;
        }
    }

    private void updateDriverMarker(GeoPoint point) {
        if (driverMarker == null) {
            driverMarker = createDriverMarker(point);
            mapView.getOverlays().add(driverMarker);
        } else {
            driverMarker.setPosition(point);
        }

        mapView.invalidate();
    }

    private void updateTrackingUi(DriverUpdate update) {
        if (update == null) {
            tvDriverName.setText(getString(R.string.not_available));
            tvVehicle.setText(getString(R.string.not_available));
            tvDistance.setText(getString(R.string.not_available));
            tvEta.setText(getString(R.string.not_available));
            tvLiveStatus.setText(getString(R.string.status_searching_driver));
            return;
        }

        if (update.driverName != null && !update.driverName.trim().isEmpty()) {
            tvDriverName.setText(update.driverName);
        } else {
            tvDriverName.setText(getString(R.string.not_available));
        }

        if (update.vehicle != null && !update.vehicle.trim().isEmpty()) {
            tvVehicle.setText(update.vehicle);
        } else {
            tvVehicle.setText(getString(R.string.not_available));
        }

        tvEta.setText((update.eta != null && !update.eta.trim().isEmpty()) ? update.eta : getString(R.string.not_available));
        tvLiveStatus.setText((update.status != null && !update.status.trim().isEmpty()) ? update.status : getString(R.string.status_tracking));

        String distanceText = getDistanceToStartText(update.driverLat, update.driverLng);
        tvDistance.setText(distanceText);
    }

    private String getDistanceToStartText(double driverLat, double driverLng) {
        if (startLocation == null) {
            return getString(R.string.not_available);
        }

        float[] results = new float[1];
        Location.distanceBetween(
            startLocation.getLatitude(),
            startLocation.getLongitude(),
            driverLat,
            driverLng,
            results
        );

        float meters = results[0];
        if (meters < 1000) {
            return getString(R.string.distance_m_format, meters);
        }

        return getString(R.string.distance_km_format, meters / 1000f);
    }

    private void autoFitMarkersIfNeeded(GeoPoint driverPoint) {
        if (startLocation == null || destinationLocation == null) {
            return;
        }

        try {
            BoundingBox current = mapView.getBoundingBox();
            if (current != null && current.contains(driverPoint.getLatitude(), driverPoint.getLongitude())) {
                return;
            }

            BoundingBox box = buildBoundingBox(startLocation, destinationLocation, driverPoint);
            if (box != null) {
                mapView.zoomToBoundingBox(box, true);
            }
        } catch (Exception ignored) {
        }
    }

    private BoundingBox buildBoundingBox(GeoPoint... points) {
        double minLat = Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;

        for (GeoPoint p : points) {
            if (p == null) {
                continue;
            }
            minLat = Math.min(minLat, p.getLatitude());
            minLng = Math.min(minLng, p.getLongitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            maxLng = Math.max(maxLng, p.getLongitude());
        }

        if (minLat == Double.MAX_VALUE) {
            return null;
        }

        return new BoundingBox(maxLat, maxLng, minLat, minLng);
    }

    private String getTrackingId() {
        if (bookingData.getBookingId() != null && !bookingData.getBookingId().trim().isEmpty()) {
            return bookingData.getBookingId();
        }
        return bookingData.getRequestId();
    }

    private String formatPrice(double price) {
        return String.format("$%.2f", price);
    }

    private static String getFirstString(JSONObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key) && !json.isNull(key)) {
                String value = json.optString(key, null);
                if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim())) {
                    return value;
                }
            }
        }
        return null;
    }

    private static double getFirstDouble(JSONObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key) && !json.isNull(key)) {
                try {
                    return json.getDouble(key);
                } catch (Exception ignored) {
                }
                try {
                    String value = json.optString(key, null);
                    if (value != null) {
                        return Double.parseDouble(value);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return Double.NaN;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }

        if (bookingState == BookingState.TRACKING) {
            startPollingDriverLocation(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }

        stopPollingDriverLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPollingDriverLocation();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("bookingState", bookingState.name());
        outState.putString("requestId", bookingData.getRequestId());
        outState.putString("bookingId", bookingData.getBookingId());
        outState.putString("apiUrl", etApiEndpoint.getText().toString());

        if (startLocation != null) {
            outState.putDouble("startLat", startLocation.getLatitude());
            outState.putDouble("startLng", startLocation.getLongitude());
        }

        if (destinationLocation != null) {
            outState.putDouble("destLat", destinationLocation.getLatitude());
            outState.putDouble("destLng", destinationLocation.getLongitude());
        }

        if (driverMarker != null) {
            GeoPoint p = driverMarker.getPosition();
            outState.putDouble("driverLat", p.getLatitude());
            outState.putDouble("driverLng", p.getLongitude());
        }
    }

    private void restoreState(Bundle state) {
        try {
            String bookingStateStr = state.getString("bookingState", BookingState.LOCATION_SELECTION.name());
            bookingState = BookingState.valueOf(bookingStateStr);
        } catch (Exception ignored) {
            bookingState = BookingState.LOCATION_SELECTION;
        }

        bookingData.setRequestId(state.getString("requestId", null));
        bookingData.setBookingId(state.getString("bookingId", null));

        String apiUrl = state.getString("apiUrl", null);
        if (apiUrl != null) {
            etApiEndpoint.setText(apiUrl);
        }

        if (state.containsKey("startLat") && state.containsKey("startLng")) {
            startLocation = new GeoPoint(state.getDouble("startLat"), state.getDouble("startLng"));
            setStartLocation(startLocation);
        }

        if (state.containsKey("destLat") && state.containsKey("destLng")) {
            destinationLocation = new GeoPoint(state.getDouble("destLat"), state.getDouble("destLng"));
            setDestinationLocation(destinationLocation);
        }

        if (state.containsKey("driverLat") && state.containsKey("driverLng")) {
            GeoPoint driver = new GeoPoint(state.getDouble("driverLat"), state.getDouble("driverLng"));
            updateDriverMarker(driver);
        }
    }
}
