# Taxi App - Price Confirmation & Real-Time Driver Tracking

## Overview

This document describes the extended features of the MapLocator taxi app, including price confirmation and real-time driver position tracking.

## New Features

### 1. Price Confirmation Flow

After the user submits start and destination locations:

1. **API Response**: The app expects a JSON response containing the taxi price
2. **Price Dialog**: A confirmation dialog displays:
   - Calculated taxi price (formatted as currency)
   - Start location coordinates
   - Destination location coordinates
   - Request ID (if provided by API)
   - "Accept" and "Cancel" buttons

3. **User Actions**:
   - **Cancel**: Returns to location selection screen, no further action
   - **Accept**: Sends confirmation to API with `{"confirmation": "yes"}` (plus requestId if available)

4. **Loading State**: Progress indicator shown during confirmation request

### 2. Real-Time Driver Position Tracking

After price acceptance:

1. **Polling Mechanism**:
   - Automatic polling every **8 seconds**
   - Uses `ScheduledExecutorService` for precise timing
   - Continues until driver arrives, user cancels, or app is paused

2. **Tracking Screen**:
   - Replaces location selection panel with tracking panel
   - Shows real-time driver information
   - Map displays all three markers: start (green), destination (red), driver (blue)

3. **Driver Information Display**:
   - **Driver Name**: Driver's name or ID
   - **Vehicle**: Vehicle information (model, plate, etc.)
   - **Distance**: Real-time distance from driver to start location
   - **ETA**: Estimated time of arrival
   - **Status**: Live status message (e.g., "On the way", "Arriving in 5 mins")
   - **Polling Status**: Shows "Active" (green) or error count (red)

4. **Map Features**:
   - Blue marker for driver position (updates every 8 seconds)
   - Auto-fit: Map automatically zooms to include all markers when driver moves out of view
   - Non-draggable driver marker (start and destination remain draggable)

5. **Cancel Booking**:
   - Red "Cancel Booking" button
   - Confirmation dialog before canceling
   - Sends cancellation request to API
   - Stops polling and returns to location selection

## API Integration

### 1. Initial Location Submission

**Request:**
```json
POST /api/endpoint
Content-Type: application/json

{
  "startLat": 37.774929,
  "startLng": -122.419416,
  "destLat": 37.804829,
  "destLng": -122.403916
}
```

**Expected Response:**
```json
{
  "price": 25.50,
  "requestId": "req_123456",
  "taxiPrice": 25.50,  // Alternative field name
  "bookingId": "booking_789"  // Alternative field name
}
```

**Flexible Field Names**:
- Price: `price`, `taxiPrice`, `amount`, or `fare`
- ID: `requestId`, `bookingId`, or `id`

### 2. Price Confirmation

**Request:**
```json
POST /api/endpoint
Content-Type: application/json

{
  "requestId": "req_123456",
  "confirmation": "yes"
}
```

**Expected Response:**
```json
{
  "bookingId": "booking_789",
  "status": "confirmed",
  "message": "Booking confirmed, searching for driver..."
}
```

**Flexible Field Names**:
- ID: `bookingId`, `requestId`, or `id`

### 3. Driver Position Polling (Every 8 Seconds)

**Request:**
```json
POST /api/endpoint
Content-Type: application/json

{
  "bookingId": "booking_789"
}
```

**Expected Response:**
```json
{
  "driverLat": 37.776500,
  "driverLng": -122.420000,
  "driverName": "John Doe",
  "vehicle": "Toyota Camry - ABC123",
  "eta": "5 mins",
  "status": "On the way"
}
```

**Flexible Field Names**:
- Latitude: `driverLat`, `lat`, or `latitude`
- Longitude: `driverLng`, `lng`, `lon`, or `longitude`
- Driver: `driverName`, `name`, or `driverId`
- Vehicle: `vehicle`, `car`, or `vehicleInfo`
- ETA: `eta`, `etaMinutes`, `etaMins`, or `etaText`
- Status: `status`, `message`, or `state`

**Automatic Stop Conditions**:
Polling automatically stops if status contains:
- "arrived"
- "completed"
- "cancelled"

### 4. Cancel Booking

**Request:**
```json
POST /api/endpoint
Content-Type: application/json

{
  "bookingId": "booking_789",
  "cancel": true
}
```

## Technical Implementation

### Threading Model

1. **Network Requests**: 
   - All HTTP requests use `AsyncTask` with `THREAD_POOL_EXECUTOR`
   - Non-blocking, won't freeze UI

2. **Polling**:
   - `ScheduledExecutorService` with single thread
   - Precise 8-second intervals using `scheduleAtFixedRate()`
   - UI updates posted to main thread via `Handler`

3. **Lifecycle Management**:
   - Polling stops on `onPause()` (app backgrounded)
   - Polling resumes on `onResume()` if in tracking state
   - Complete cleanup on `onDestroy()`

### State Management

**Three States:**
1. **LOCATION_SELECTION**: Initial state, user selects locations
2. **WAITING_PRICE_CONFIRMATION**: Price dialog shown, awaiting user decision
3. **TRACKING**: Driver tracking active, polling running

**State Persistence:**
- All state saved in `onSaveInstanceState()`
- Restored in `restoreState()` on configuration changes
- Handles screen rotation without losing booking

### Error Handling

1. **Network Errors**:
   - Consecutive error count displayed
   - Toast notification on first error and every 3rd error
   - Polling continues despite errors

2. **Invalid Responses**:
   - Flexible JSON parsing tries multiple field names
   - Falls back to defaults if fields missing
   - User-friendly error messages

3. **Missing Data**:
   - Shows "N/A" for unavailable fields
   - Continues tracking with partial data

### Distance Calculation

Uses Android's `Location.distanceBetween()`:
- Calculates great-circle distance
- Returns distance from driver to start location
- Formatted as meters (< 1000m) or kilometers (≥ 1000m)

### Map Auto-Fitting

- Checks if driver marker is within current view bounds
- If outside, calculates bounding box for all 3 markers
- Zooms map to show all markers with padding
- Only adjusts if necessary (doesn't zoom on every update)

## User Experience

### Flow

1. User selects start and destination on map
2. User enters API endpoint URL
3. User taps "Submit Locations"
4. App sends location data to API
5. **Price confirmation dialog appears**
6. If user clicks "Cancel" → back to step 1
7. If user clicks "Accept" → app sends confirmation
8. **Tracking screen appears**
9. App polls for driver position every 8 seconds
10. Map shows driver moving in real-time
11. User can cancel booking at any time
12. Polling stops when driver arrives

### UI Elements

**Location Selection Screen:**
- Start/Destination coordinate displays
- API endpoint input field
- "Set Start Location" button
- "Set Destination Location" button
- "Submit Locations" button (green)
- Progress bar (during submission)
- Status text

**Tracking Screen:**
- Title: "Driver tracking"
- Driver information panel:
  - Driver name
  - Vehicle info
  - Distance to pickup
  - Estimated time of arrival
  - Live status message
  - Polling status indicator
- "Cancel Booking" button (red)

### Visual Design

**Markers:**
- 🟢 Green: Start location
- 🔴 Red: Destination location
- 🔵 Blue: Driver position

**Colors:**
- Green: Active/success states
- Red: Errors/cancel actions
- Gray: Inactive states
- Blue: Driver-related elements

## Testing

### Mock API Responses

**Price Response:**
```json
{
  "price": 15.00,
  "requestId": "test_123"
}
```

**Confirmation Response:**
```json
{
  "bookingId": "book_456",
  "status": "confirmed"
}
```

**Driver Location Response (cycle through positions):**
```json
{
  "driverLat": 37.7750,
  "driverLng": -122.4195,
  "driverName": "Test Driver",
  "vehicle": "Test Car",
  "eta": "3 mins",
  "status": "On the way"
}
```

### Testing Scenarios

1. **Happy Path**:
   - Submit locations → Accept price → Watch driver approach → Driver arrives

2. **User Cancellation**:
   - Submit locations → Accept price → Cancel booking

3. **Price Rejection**:
   - Submit locations → Reject price → Back to location selection

4. **Network Errors**:
   - No internet during submission
   - Timeout during polling
   - Invalid API responses

5. **App Lifecycle**:
   - Rotate device during tracking
   - Background app during tracking (polling stops)
   - Return to app (polling resumes)

6. **Edge Cases**:
   - Missing optional fields in API response
   - Driver status changes to "arrived"
   - Multiple consecutive polling errors

## Configuration

### Polling Interval

Change in `MainActivity.java`:
```java
private static final long DRIVER_POLL_INTERVAL_SECONDS = 8;
```

### Timeout

Change in `NetworkHelper.java`:
```java
private static final int TIMEOUT_MS = 15000;
```

### Default Map Center

Change in `MainActivity.java`:
```java
GeoPoint startPoint = new GeoPoint(37.7749, -122.4194); // San Francisco
```

## Performance Considerations

1. **Network Usage**:
   - ~1 KB per polling request
   - 450 KB per hour of tracking (7.5 requests/min)
   - Map tiles cached after first load

2. **Battery Impact**:
   - Moderate: Network requests every 8 seconds
   - Polling stops when app is backgrounded
   - No GPS usage (uses API-provided coordinates)

3. **Memory**:
   - Lightweight: Only 3 markers on map
   - No history stored
   - Previous driver positions not kept

## Known Limitations

1. **No Route Drawing**: Only markers shown, no line between points
2. **Single Booking**: One booking at a time
3. **No History**: Past bookings not saved
4. **No Authentication**: API requests have no auth headers
5. **AsyncTask**: Using deprecated AsyncTask (works but could be modernized)

## Future Enhancements

1. Route polyline between driver and pickup location
2. Distance/time to destination
3. Booking history
4. Push notifications for driver updates
5. Chat with driver
6. Rating system after ride
7. Multiple waypoints
8. Fare estimation before booking
9. Payment integration
10. Driver photo/profile

## Troubleshooting

**Polling not working:**
- Check API endpoint URL is correct
- Verify API returns valid JSON with lat/lng
- Check network connectivity
- Look for error toast messages

**Driver marker not moving:**
- Verify API returns different coordinates each poll
- Check polling status in UI (should show "Active" in green)
- Ensure app is in foreground

**Price dialog not showing:**
- API must return valid JSON with "price" field (or variant)
- Check response in Logcat with tag "NetworkHelper"
- Verify response code is 200-299

**App crashes on rotation:**
- Should not happen, state is saved/restored
- Report bug with stack trace if it does

## Support

For issues or questions:
1. Check Logcat for "NetworkHelper" and "MainActivity" tags
2. Verify API responses match expected format
3. Test with mock API (httpbin.org)
4. Check network connectivity

---

**Version**: 2.0  
**Last Updated**: 2025-12-26  
**Polling Interval**: 8 seconds  
**Android Min SDK**: 21 (Lollipop)  
**Android Target SDK**: 34 (Android 14)
