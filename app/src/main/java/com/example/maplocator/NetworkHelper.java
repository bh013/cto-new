package com.example.maplocator;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NetworkHelper {

    private static final String TAG = "NetworkHelper";
    private static final int TIMEOUT_MS = 15000;

    public interface NetworkCallback {
        void onSuccess(String response);

        void onError(String error);
    }

    private static class NetworkResult {
        final boolean success;
        final String message;

        NetworkResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private static class JsonRequestTask extends AsyncTask<Void, Void, NetworkResult> {

        private final String apiUrl;
        private final String method;
        private final JSONObject jsonBody;
        private final NetworkCallback callback;

        JsonRequestTask(String apiUrl, String method, JSONObject jsonBody, NetworkCallback callback) {
            this.apiUrl = apiUrl;
            this.method = method;
            this.jsonBody = jsonBody;
            this.callback = callback;
        }

        @Override
        protected NetworkResult doInBackground(Void... voids) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                String jsonString = jsonBody != null ? jsonBody.toString() : "";
                Log.d(TAG, "Request URL: " + apiUrl);
                Log.d(TAG, "Request Method: " + method);
                if (jsonBody != null) {
                    Log.d(TAG, "Request Body: " + jsonString);
                }

                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);

                if (jsonBody != null) {
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setDoInput(true);

                    OutputStream outputStream = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
                    );
                    writer.write(jsonString);
                    writer.flush();
                    writer.close();
                    outputStream.close();
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                    );
                } else {
                    reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)
                    );
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String responseBody = response.toString();
                Log.d(TAG, "Response Body: " + responseBody);

                if (responseCode >= 200 && responseCode < 300) {
                    return new NetworkResult(true, responseBody);
                }

                return new NetworkResult(false, "HTTP " + responseCode + ": " + responseBody);

            } catch (Exception e) {
                Log.e(TAG, "Network error", e);
                return new NetworkResult(false, "Error: " + e.getMessage());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error closing reader", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(NetworkResult result) {
            if (callback == null) {
                return;
            }

            if (result.success) {
                callback.onSuccess(result.message);
            } else {
                callback.onError(result.message);
            }
        }
    }

    private static void postJson(String apiUrl, JSONObject jsonBody, NetworkCallback callback) {
        new JsonRequestTask(apiUrl, "POST", jsonBody, callback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void postLocationData(String apiUrl, double startLat, double startLng,
                                        double destLat, double destLng, NetworkCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("startLat", startLat);
            jsonBody.put("startLng", startLng);
            jsonBody.put("destLat", destLat);
            jsonBody.put("destLng", destLng);
            postJson(apiUrl, jsonBody, callback);
        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Error: " + e.getMessage());
            }
        }
    }

    public static void postPriceConfirmation(String apiUrl, String requestId, NetworkCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            if (requestId != null && !requestId.trim().isEmpty()) {
                jsonBody.put("requestId", requestId);
            }
            jsonBody.put("confirmation", "yes");
            postJson(apiUrl, jsonBody, callback);
        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Error: " + e.getMessage());
            }
        }
    }

    public static void pollDriverPosition(String apiUrl, String bookingId, NetworkCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            if (bookingId != null && !bookingId.trim().isEmpty()) {
                jsonBody.put("bookingId", bookingId);
            }
            postJson(apiUrl, jsonBody, callback);
        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Error: " + e.getMessage());
            }
        }
    }

    public static void cancelBooking(String apiUrl, String bookingId, NetworkCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            if (bookingId != null && !bookingId.trim().isEmpty()) {
                jsonBody.put("bookingId", bookingId);
            }
            jsonBody.put("cancel", true);
            postJson(apiUrl, jsonBody, callback);
        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Error: " + e.getMessage());
            }
        }
    }
}
