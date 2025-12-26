# MapLocator - Android Location Selection App

A complete Android application that allows users to select start and destination locations on an interactive OpenStreetMap and submit them to a configurable API endpoint.

## Features

### 1. Interactive Map Display
- Uses **Osmdroid** library to display OpenStreetMap
- Default center location: San Francisco (37.7749, -122.4194)
- Smooth pan and zoom controls
- Multi-touch gestures support

### 2. Location Selection
- **Start Location**: Tap "Set Start Location" button, then tap on the map
- **Destination Location**: Tap "Set Destination Location" button, then tap on the map
- Visual markers for both positions (green for start, red for destination)
- **Draggable markers** for fine-tuning positions
- Real-time coordinate display (latitude, longitude)

### 3. Data Submission
- Configurable API endpoint URL input field
- Submit button to send locations via HTTP POST
- Uses **pure Java HttpURLConnection** (no third-party HTTP libraries)
- JSON payload format:
  ```json
  {
    "startLat": 37.774929,
    "startLng": -122.419416,
    "destLat": 37.804829,
    "destLng": -122.403916
  }
  ```

### 4. Technical Implementation

#### Network Layer
- **NetworkHelper.java**: Custom class using `HttpURLConnection`
- POST requests with JSON body
- Timeout handling (15 seconds)
- Request/response logging
- AsyncTask for background execution
- Proper error handling

#### Permissions
- `INTERNET` - Network access
- `ACCESS_FINE_LOCATION` - Precise location
- `ACCESS_COARSE_LOCATION` - Approximate location
- `WRITE_EXTERNAL_STORAGE` - Map tile caching (API ≤ 32)
- `READ_EXTERNAL_STORAGE` - Map tile caching (API ≤ 32)

#### UI Components
- MapView (Osmdroid)
- EditText for API endpoint
- Buttons for location selection and submission
- TextViews for coordinate display and status
- ProgressBar for loading indication

## Project Structure

```
MapLocator/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/example/maplocator/
│           │   ├── MainActivity.java
│           │   └── NetworkHelper.java
│           └── res/
│               ├── drawable/
│               │   └── ic_launcher_foreground.xml
│               ├── layout/
│               │   └── activity_main.xml
│               ├── mipmap-*/
│               │   └── ic_launcher*.png
│               └── values/
│                   ├── colors.xml
│                   ├── strings.xml
│                   └── ic_launcher_background.xml
├── build.gradle
├── gradle.properties
├── settings.gradle
└── README.md
```

## Dependencies

```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.10.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'org.osmdroid:osmdroid-android:6.1.17'
```

## Build Requirements

- **Android Studio**: Arctic Fox or later
- **Gradle**: 8.1.0
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Java**: 8

## CI/CD & Releases

This project includes **GitHub Actions workflows** for automated building and releasing:

### 📦 Download APK
- Go to **[Releases](../../releases)** to download the latest APK
- Both debug and release versions available
- Includes checksums for verification

### 🔄 Automated Workflows
- **Automatic builds** on push/pull requests
- **GitHub Releases** on version tags
- **Manual builds** on demand

For more information:
- **Quick Start**: [.github/workflows/QUICKSTART.md](.github/workflows/QUICKSTART.md)
- **Release Guide**: [RELEASE_GUIDE.md](RELEASE_GUIDE.md)
- **Full Documentation**: [.github/workflows/README.md](.github/workflows/README.md)
- **CI/CD Setup**: [CI_CD_SETUP.md](CI_CD_SETUP.md)

## How to Use

1. **Launch the app**
2. **Set Start Location**:
   - Tap "Set Start Location" button
   - Tap anywhere on the map
   - Marker appears (green, draggable)
   - Coordinates display updates
3. **Set Destination Location**:
   - Tap "Set Destination Location" button
   - Tap anywhere on the map
   - Marker appears (red, draggable)
   - Coordinates display updates
4. **Configure API Endpoint**:
   - Enter your API endpoint URL in the text field
   - Example: `https://your-api.com/locations`
5. **Submit**:
   - Tap "Submit Locations" button
   - Progress bar appears
   - Success or error message displays

## API Integration

### Request Format

**Method**: POST  
**Content-Type**: application/json  
**Body**:
```json
{
  "startLat": 37.774929,
  "startLng": -122.419416,
  "destLat": 37.804829,
  "destLng": -122.403916
}
```

### Response Handling

- **Success (2xx)**: Displays response body
- **Error (4xx/5xx)**: Displays HTTP status and error message
- **Network Error**: Displays exception message

### Logging

All network requests and responses are logged with tag `NetworkHelper`:
```
D/NetworkHelper: Request URL: https://your-api.com/locations
D/NetworkHelper: Request Body: {"startLat":37.774929,...}
D/NetworkHelper: Response Code: 200
D/NetworkHelper: Response Body: {"success":true}
```

## Code Highlights

### MainActivity.java
- Map initialization and configuration
- Marker creation with drag support
- Touch event handling
- Permission management
- UI state management

### NetworkHelper.java
- Pure HttpURLConnection implementation
- AsyncTask for background threading
- JSON payload construction
- Response parsing
- Error handling
- Callback interface for async results

## Error Handling

- **Missing Locations**: Toast message if start/destination not set
- **Missing API URL**: Toast message if endpoint not provided
- **Network Errors**: Displayed in status text and toast
- **Permission Denial**: Toast warning message
- **HTTP Errors**: Status code and message displayed

## Testing

To test without a real API:
1. Set up a local server or use a service like [RequestBin](https://requestbin.com/)
2. Enter the endpoint URL
3. Set locations and submit
4. Check logs for request/response details

## Future Enhancements

- Current location detection (GPS)
- Route drawing between points
- Search functionality
- Location history
- Multiple waypoints
- Distance calculation
- Custom marker icons

## License

This is a demonstration project created for educational purposes.
