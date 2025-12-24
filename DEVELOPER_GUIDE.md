# Developer Guide - MapLocator Android App

## Quick Start

### Prerequisites
- JDK 8 or higher
- Android SDK (API 21-34)
- Android device or emulator

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Check for issues
./gradlew lint
```

## Code Architecture

### Package Structure
```
com.example.maplocator/
├── MainActivity.java      - Main UI and map logic
└── NetworkHelper.java     - HTTP networking layer
```

### MainActivity Overview

**Responsibilities**:
- Map initialization and rendering
- User interaction handling (taps, drags)
- Marker management (create, update, remove)
- Permission requests
- UI state management
- Network callback handling

**Key Components**:
- `MapView mapView` - Osmdroid map instance
- `Marker startMarker` - Green start location marker
- `Marker destinationMarker` - Red destination marker
- `GeoPoint startLocation` - Start coordinates
- `GeoPoint destinationLocation` - Destination coordinates
- `SelectionMode currentMode` - Tracks what user is selecting

**Lifecycle Methods**:
- `onCreate()` - Initialize views, request permissions, setup map
- `onResume()` - Resume map rendering
- `onPause()` - Pause map rendering
- `onRequestPermissionsResult()` - Handle permission responses

**Core Methods**:
```java
setupMap()                    // Initialize Osmdroid configuration
handleMapTap(GeoPoint)        // Process tap on map
setStartLocation(GeoPoint)    // Place start marker
setDestinationLocation(GeoPoint) // Place destination marker
createMarker(...)             // Create draggable marker with icon
submitLocations()             // Validate and send data
```

### NetworkHelper Overview

**Responsibilities**:
- HTTP POST request construction
- JSON payload creation
- Timeout management
- Response parsing
- Error handling
- Background thread execution

**Key Components**:
- `PostLocationTask` - AsyncTask for network calls
- `NetworkCallback` - Interface for async results
- `NetworkResult` - Internal result wrapper

**Usage Example**:
```java
NetworkHelper.postLocationData(
    apiUrl,
    startLat, startLng,
    destLat, destLng,
    new NetworkHelper.NetworkCallback() {
        @Override
        public void onSuccess(String response) {
            // Handle success
        }
        
        @Override
        public void onError(String error) {
            // Handle error
        }
    }
);
```

## Key Concepts

### 1. Osmdroid Map Configuration

```java
// Set tile source
mapView.setTileSource(TileSourceFactory.MAPNIK);

// Enable gestures
mapView.setMultiTouchControls(true);

// Set zoom controls
mapView.setBuiltInZoomControls(true);

// Set initial position
IMapController controller = mapView.getController();
controller.setZoom(13.0);
controller.setCenter(new GeoPoint(lat, lng));
```

### 2. Handling Map Taps

```java
MapEventsReceiver receiver = new MapEventsReceiver() {
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        // Process tap at GeoPoint p
        handleMapTap(p);
        return true;
    }
    
    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }
};

MapEventsOverlay overlay = new MapEventsOverlay(receiver);
mapView.getOverlays().add(0, overlay);
```

### 3. Creating Draggable Markers

```java
Marker marker = new Marker(mapView);
marker.setPosition(geoPoint);
marker.setDraggable(true);
marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
    @Override
    public void onMarkerDragEnd(Marker marker) {
        GeoPoint newPosition = marker.getPosition();
        // Update location and UI
    }
    
    @Override
    public void onMarkerDragStart(Marker marker) {}
    
    @Override
    public void onMarkerDrag(Marker marker) {}
});

mapView.getOverlays().add(marker);
mapView.invalidate();
```

### 4. HttpURLConnection POST Request

```java
URL url = new URL(apiUrl);
HttpURLConnection connection = (HttpURLConnection) url.openConnection();

// Configure request
connection.setRequestMethod("POST");
connection.setRequestProperty("Content-Type", "application/json");
connection.setDoOutput(true);
connection.setConnectTimeout(15000);
connection.setReadTimeout(15000);

// Write JSON body
OutputStream out = connection.getOutputStream();
BufferedWriter writer = new BufferedWriter(
    new OutputStreamWriter(out, StandardCharsets.UTF_8)
);
writer.write(jsonString);
writer.flush();
writer.close();

// Read response
int responseCode = connection.getResponseCode();
BufferedReader reader = new BufferedReader(
    new InputStreamReader(connection.getInputStream())
);
String line;
StringBuilder response = new StringBuilder();
while ((line = reader.readLine()) != null) {
    response.append(line);
}

connection.disconnect();
```

## Customization Guide

### Change Default Map Location
**File**: `MainActivity.java`  
**Method**: `setupMap()`  
**Line**: ~135

```java
// Change from San Francisco to New York
GeoPoint startPoint = new GeoPoint(40.7128, -74.0060);
mapController.setCenter(startPoint);
```

### Change Marker Colors
**File**: `MainActivity.java`  
**Method**: `createMarker()`  
**Lines**: ~198-204

```java
if (isStart) {
    marker.setIcon(getMarkerIcon(Color.BLUE)); // Change from GREEN
} else {
    marker.setIcon(getMarkerIcon(Color.YELLOW)); // Change from RED
}
```

### Change Network Timeout
**File**: `NetworkHelper.java`  
**Line**: 20

```java
private static final int TIMEOUT_MS = 30000; // Change from 15000
```

### Add Request Headers
**File**: `NetworkHelper.java`  
**Method**: `doInBackground()`  
**After line**: 67

```java
connection.setRequestProperty("Authorization", "Bearer " + token);
connection.setRequestProperty("X-Custom-Header", "value");
```

### Modify JSON Payload
**File**: `NetworkHelper.java`  
**Method**: `doInBackground()`  
**Lines**: 52-56

```java
JSONObject jsonBody = new JSONObject();
jsonBody.put("startLat", startLat);
jsonBody.put("startLng", startLng);
jsonBody.put("destLat", destLat);
jsonBody.put("destLng", destLng);
jsonBody.put("timestamp", System.currentTimeMillis()); // Add timestamp
jsonBody.put("userId", getUserId());                   // Add user ID
```

## Debugging

### Enable Verbose Logging
```bash
adb logcat -s NetworkHelper:V MainActivity:V
```

### View All App Logs
```bash
adb logcat | grep -E "com.example.maplocator"
```

### Monitor Network Traffic
Use Android Studio's Network Profiler or Charles Proxy.

### Common Issues

#### 1. Map not displaying
- Check INTERNET permission
- Check WRITE_EXTERNAL_STORAGE permission
- Verify device has internet connection
- Check logcat for Osmdroid errors

#### 2. Markers not appearing
- Call `mapView.invalidate()` after adding marker
- Check marker position is within valid lat/lng range
- Ensure marker is added to map overlays

#### 3. Network request fails
- Verify API URL is correct (include http:// or https://)
- Check INTERNET permission granted
- Ensure device has network connectivity
- Check logcat for exception details

#### 4. Permission denied
- Target API 23+ requires runtime permissions
- Call `requestPermissions()` at startup
- Handle `onRequestPermissionsResult()` callback

## Testing

### Unit Testing NetworkHelper
```java
// Mock test (would require additional dependencies)
@Test
public void testPostLocationData() {
    String mockResponse = "{\"success\":true}";
    // Test network callback
}
```

### Manual Testing Checklist
- [ ] Map displays on launch
- [ ] Can pan and zoom map
- [ ] "Set Start Location" button works
- [ ] Tap on map places green marker
- [ ] Start marker is draggable
- [ ] Start coordinates display updates
- [ ] "Set Destination Location" button works
- [ ] Tap on map places red marker
- [ ] Destination marker is draggable
- [ ] Destination coordinates display updates
- [ ] Can enter API endpoint URL
- [ ] Submit without locations shows error
- [ ] Submit without URL shows error
- [ ] Submit with valid data sends request
- [ ] Progress bar shows during request
- [ ] Success message displays on success
- [ ] Error message displays on failure
- [ ] App doesn't crash on network error

### Test with Mock API
```bash
# Using httpbin.org
URL: https://httpbin.org/post

# Using local server
json-server --watch db.json --port 3000
URL: http://10.0.2.2:3000/locations  # For emulator
```

## Performance Optimization

### Map Performance
- Use appropriate zoom level (don't start at zoom 20)
- Limit number of overlays on map
- Remove old markers before adding new ones
- Use `mapView.onPause()` when app backgrounds

### Network Performance
- Current timeout: 15 seconds (reasonable)
- Consider adding request caching
- Implement retry logic for failures
- Use GZIP compression for large payloads

### Memory Management
- Properly cleanup markers
- Call `mapView.onDetach()` in `onDestroy()`
- Avoid memory leaks in AsyncTask callbacks
- Use weak references if storing context

## Security Considerations

### Current Implementation
- ✅ HTTPS supported
- ✅ No sensitive data stored locally
- ✅ Permissions requested at runtime
- ⚠️ API endpoint user-provided (validate in production)
- ⚠️ No authentication implemented

### Production Recommendations
1. **API Endpoint**: Hardcode or validate URL format
2. **Authentication**: Add token-based auth
3. **Data Validation**: Validate coordinates before sending
4. **SSL Pinning**: Implement for production API
5. **ProGuard**: Enable for release builds
6. **API Keys**: Store in BuildConfig or secure storage

## Migration Notes

### AsyncTask Deprecation
AsyncTask is deprecated in API 30+. Consider migration to:

**Option 1: ExecutorService**
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Handler handler = new Handler(Looper.getMainLooper());

executor.execute(() -> {
    String result = doNetworkCall();
    handler.post(() -> {
        updateUI(result);
    });
});
```

**Option 2: Kotlin Coroutines** (requires Kotlin)
```kotlin
lifecycleScope.launch {
    val result = withContext(Dispatchers.IO) {
        doNetworkCall()
    }
    updateUI(result)
}
```

**Option 3: RxJava** (requires RxJava dependency)
```java
Observable.fromCallable(() -> doNetworkCall())
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(result -> updateUI(result));
```

## Contributing

### Code Style
- Follow Android Java conventions
- Use meaningful variable names
- Add comments for complex logic
- Keep methods under 50 lines when possible
- Extract repeated code into methods

### Pull Request Guidelines
1. Create feature branch from `main`
2. Write clear commit messages
3. Test on multiple Android versions
4. Update README if needed
5. Ensure no lint warnings

## Resources

### Documentation
- [Osmdroid Wiki](https://github.com/osmdroid/osmdroid/wiki)
- [HttpURLConnection Guide](https://developer.android.com/reference/java/net/HttpURLConnection)
- [Android Permissions](https://developer.android.com/guide/topics/permissions/overview)
- [Android AsyncTask](https://developer.android.com/reference/android/os/AsyncTask)

### Related Libraries
- Osmdroid: https://github.com/osmdroid/osmdroid
- AndroidX: https://developer.android.com/jetpack/androidx

## Support

For issues or questions:
1. Check logcat output
2. Review this guide
3. Search existing issues
4. Create new issue with details
