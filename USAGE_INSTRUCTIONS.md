# MapLocator - Usage Instructions

## Overview
MapLocator is an Android application that allows you to select two locations on a map and send them to a web API.

## Features
- 🗺️ Interactive OpenStreetMap
- 📍 Set Start and Destination locations by tapping the map
- ✋ Drag markers to adjust positions
- 🌐 Submit locations to any API endpoint
- 📊 Real-time coordinate display

## Installation

### Method 1: Build from Source
1. Install Android Studio
2. Open this project in Android Studio
3. Connect your Android device or start an emulator
4. Click Run ▶️ button
5. Grant permissions when prompted

### Method 2: Install APK
1. Build the APK: `./gradlew assembleDebug`
2. Find APK at: `app/build/outputs/apk/debug/app-debug.apk`
3. Transfer to your Android device
4. Install the APK (enable "Install from Unknown Sources" if needed)

## First Launch

When you first open the app, it will request several permissions:

### Required Permissions
- **Internet** - To download map tiles and send API requests
- **Location** - To optionally detect your current location
- **Storage** - To cache map tiles for offline viewing

**Important**: Grant all permissions for the app to work correctly.

## How to Use

### Step 1: Open the App
Launch MapLocator from your app drawer. You'll see an interactive map centered on San Francisco.

### Step 2: Navigate the Map
- **Pan**: Touch and drag to move around
- **Zoom In**: Pinch outward or tap + button
- **Zoom Out**: Pinch inward or tap - button

### Step 3: Set Start Location
1. Tap the **"Set Start Location"** button
2. The button will highlight green
3. Tap anywhere on the map where you want the start point
4. A **green marker** will appear
5. The coordinates will display at the top (e.g., "Start: 37.774929, -122.419416")

### Step 4: Adjust Start Location (Optional)
- **Touch and drag** the green marker to fine-tune its position
- The coordinates will update automatically as you drag

### Step 5: Set Destination Location
1. Tap the **"Set Destination Location"** button
2. The button will highlight green
3. Tap anywhere on the map where you want the destination
4. A **red marker** will appear
5. The coordinates will display at the top (e.g., "Dest: 37.804829, -122.403916")

### Step 6: Adjust Destination (Optional)
- **Touch and drag** the red marker to fine-tune its position
- The coordinates will update automatically as you drag

### Step 7: Enter API Endpoint
In the text field labeled "API Endpoint URL", enter the URL where you want to send the location data.

**Example URLs**:
- Testing: `https://httpbin.org/post`
- Your API: `https://yourapi.com/locations`
- Local server: `http://192.168.1.100:3000/api/locations`

### Step 8: Submit Locations
1. Tap the **"Submit Locations"** button (green button)
2. A progress bar will appear
3. Wait for the response

### Step 9: View Result
- **Success**: Green status message "Data submitted successfully!"
- **Error**: Red status message with error details

## Understanding the Display

### Coordinate Format
Coordinates are shown in decimal degrees:
```
Start: 37.774929, -122.419416
       ↑          ↑
    Latitude   Longitude
```

- **Latitude**: -90 to +90 (North-South position)
- **Positive** = North, **Negative** = South
  
- **Longitude**: -180 to +180 (East-West position)
- **Positive** = East, **Negative** = West

### Markers
- 🟢 **Green Marker** = Start location
- 🔴 **Red Marker** = Destination location

### Status Messages
- "Tap 'Set Start' or 'Set Destination' then tap on the map" - Ready to begin
- "Tap on the map to set start location" - Waiting for start tap
- "Tap on the map to set destination location" - Waiting for destination tap
- "Start location set" - Start marker placed
- "Destination location set" - Destination marker placed
- "Submitting data..." - Sending request
- "Data submitted successfully!" - Request succeeded
- "Network error: ..." - Request failed

## Data Format

When you submit, the app sends this JSON to your API:

```json
{
  "startLat": 37.774929,
  "startLng": -122.419416,
  "destLat": 37.804829,
  "destLng": -122.403916
}
```

## Testing Without a Real API

If you don't have an API yet, use this free testing service:

**URL**: `https://httpbin.org/post`

This will echo back your request, allowing you to verify the app works correctly.

## Common Issues

### Issue: Map shows gray tiles
**Cause**: No internet connection  
**Solution**: Connect to WiFi or mobile data

### Issue: "Please set both start and destination locations"
**Cause**: One or both markers not placed  
**Solution**: Tap both "Set Start" and "Set Destination" buttons and place markers

### Issue: "Please enter API endpoint URL"
**Cause**: API URL field is empty  
**Solution**: Enter a valid URL (must include http:// or https://)

### Issue: "Network error: ..."
**Causes**:
- Invalid URL format
- No internet connection
- API server is down
- Timeout (server took too long to respond)

**Solutions**:
- Check URL is correct
- Verify internet connection
- Try again later
- Contact API administrator

### Issue: Markers won't move
**Cause**: Markers are draggable, but you need to hold and drag  
**Solution**: **Long press** on marker, then drag

### Issue: App asks for permissions again
**Cause**: Permissions were denied  
**Solution**: Go to Settings > Apps > MapLocator > Permissions and enable all

## Advanced Usage

### Using with Local Development Server

If you're running a server on your computer:

**For Emulator**:
```
Use: http://10.0.2.2:3000/api/locations
(10.0.2.2 is the emulator's gateway to your computer)
```

**For Physical Device**:
```
Use: http://192.168.1.XXX:3000/api/locations
(Replace XXX with your computer's local IP address)
```

To find your computer's IP:
- **Windows**: `ipconfig`
- **Mac/Linux**: `ifconfig`

### Clearing Marker

To remove a marker and start over:
1. Simply tap the respective button again
2. Place a new marker
3. The old marker will be automatically replaced

### Viewing Request Details

If you're a developer, you can view detailed logs:
```bash
adb logcat | grep NetworkHelper
```

This shows:
- Request URL
- Request body (JSON)
- Response code
- Response body

## API Integration Guide

### For API Developers

Your API should:

1. **Accept POST requests**
2. **Expect Content-Type**: `application/json`
3. **Parse JSON body** with fields: `startLat`, `startLng`, `destLat`, `destLng`
4. **Return HTTP 200** for success
5. **Return HTTP 4xx/5xx** for errors

**Example Node.js/Express**:
```javascript
app.post('/locations', (req, res) => {
  const { startLat, startLng, destLat, destLng } = req.body;
  
  // Validate
  if (!startLat || !startLng || !destLat || !destLng) {
    return res.status(400).json({ error: 'Missing coordinates' });
  }
  
  // Process locations
  console.log('Start:', startLat, startLng);
  console.log('Destination:', destLat, destLng);
  
  // Respond
  res.json({ success: true, message: 'Locations received' });
});
```

**Example Python/Flask**:
```python
@app.route('/locations', methods=['POST'])
def receive_locations():
    data = request.json
    
    start_lat = data.get('startLat')
    start_lng = data.get('startLng')
    dest_lat = data.get('destLat')
    dest_lng = data.get('destLng')
    
    if not all([start_lat, start_lng, dest_lat, dest_lng]):
        return jsonify({'error': 'Missing coordinates'}), 400
    
    print(f'Start: {start_lat}, {start_lng}')
    print(f'Destination: {dest_lat}, {dest_lng}')
    
    return jsonify({'success': True, 'message': 'Locations received'})
```

## Tips & Tricks

### 1. Precise Marker Placement
- Zoom in close before placing markers
- Use drag to fine-tune position
- Coordinates show 6 decimal places (precise to ~0.1 meters)

### 2. Quick Reset
- Tap button and place new marker to replace old one
- No need to manually delete

### 3. Save API URL
The app doesn't save your API URL, so:
- Note it down for repeated use
- Or use a URL shortener for convenience

### 4. Offline Map Viewing
After visiting an area once:
- Map tiles are cached
- Can view that area without internet
- Still need internet to submit data

### 5. Battery Saving
- App pauses map rendering when you switch away
- Close app when not in use to save battery

## Privacy & Security

### Data Collection
- ✅ No personal data collected
- ✅ No location tracking
- ✅ No analytics or telemetry
- ✅ No ads

### Data Transmission
- ⚠️ Only sends data when YOU tap "Submit"
- ⚠️ Data sent to URL YOU provide
- ⚠️ Uses HTTPS if your URL starts with https://

### Permissions Usage
- **Internet**: Download map tiles, send API requests
- **Location**: Not actively used (reserved for future GPS feature)
- **Storage**: Cache map tiles only

## FAQ

**Q: Is this app free?**  
A: Yes, completely free and open source.

**Q: Do I need an account?**  
A: No account required.

**Q: Can I use this offline?**  
A: Map viewing works offline for cached areas, but submission requires internet.

**Q: What map data is used?**  
A: OpenStreetMap (open source, community-maintained)

**Q: Can I select more than 2 locations?**  
A: Currently limited to start and destination. Feature request for multiple waypoints noted.

**Q: Why does it ask for location permission if it doesn't use GPS?**  
A: The permission is requested but not actively used. Reserved for future GPS feature.

**Q: Can I share my selected locations?**  
A: Currently only via API submission. Future version may add share button.

**Q: Is my API URL saved?**  
A: No, you must re-enter it each time you open the app.

**Q: Can I use this with Google Maps API?**  
A: This app uses OpenStreetMap. Data can be submitted to any API that accepts the JSON format.

## Support

### Reporting Issues
If you encounter bugs:
1. Note what you were doing
2. Copy any error messages
3. Check logcat if you're a developer
4. Report with details

### Feature Requests
Suggestions for improvements:
- Multiple waypoints
- GPS location detection
- Route drawing
- Address search
- Location history
- Export to file

## Version Information

**Current Version**: 1.0  
**Build Date**: 2025-12-24  
**Min Android**: 5.0 (API 21)  
**Target Android**: 14 (API 34)

## Credits

- **Map Data**: OpenStreetMap contributors
- **Map Library**: Osmdroid (Apache License 2.0)
- **Icons**: Material Design

---

**Enjoy using MapLocator! 🗺️📍**
