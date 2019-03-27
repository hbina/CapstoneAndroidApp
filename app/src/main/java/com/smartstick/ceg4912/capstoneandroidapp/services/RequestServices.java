package com.smartstick.ceg4912.capstoneandroidapp.services;

import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;
import com.smartstick.ceg4912.capstoneandroidapp.R;
import com.smartstick.ceg4912.capstoneandroidapp.listener.BearingListener;
import com.smartstick.ceg4912.capstoneandroidapp.model.BearingRequest;
import com.smartstick.ceg4912.capstoneandroidapp.model.DirectionRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.smartstick.ceg4912.capstoneandroidapp.services.RfidServices.filterBluetooth;

public class RequestServices extends Services {

    private final static String SMART_STICK_URL_DIRECTION = "http://Capstone4913-env.rpwrn4wmqm.us-east-2.elasticbeanstalk.com/path/getDirection";
    private final static String SMART_STICK_URL_PATH = "http://Capstone4913-env.rpwrn4wmqm.us-east-2.elasticbeanstalk.com/path";
    private final static String TAG = "RequestServices";

    private final static ConcurrentLinkedQueue<BearingRequest> bearingQueue = new ConcurrentLinkedQueue<>();
    private final static ConcurrentLinkedQueue<DirectionRequest> directionQueue = new ConcurrentLinkedQueue<>();

    private final RequestQueue requestQueue;
    private final MainActivity callerActivity;

    public RequestServices(MainActivity callerActivity) {
        this.callerActivity = callerActivity;
        requestQueue = Volley.newRequestQueue(callerActivity);
    }

    private void getBearingFromDb(final String currentRFID, final String nextNode, final String currentBearing) {
        Log.d(this.toString(), String.format("BearingRequest from currentRFID:%s to nextNode:%s\n", currentRFID, nextNode));
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL_DIRECTION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Received bearing response:" + response);
                        try {
                            JSONObject reader = (new JSONObject(response)).getJSONObject("Navigation");

                            String direction = reader.getString("direction");
                            int bearing = reader.getInt("bearingDestination");
                            ((TextView) callerActivity.findViewById(R.id.di_content_direction)).setText(direction);
                            String toSpeak = String.format(Locale.ENGLISH, "turn %d degrees %s to get to %s", bearing, direction, nextNode);
                            Log.d(TAG, toSpeak);
                            SpeechServices.addText(toSpeak);
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Log.d(TAG,
                        (e == null || e.getMessage() == null) ? "An unexpected error have occured" : e
                                .getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> pars = new HashMap<>();
                pars.put("Content-Type", "application/x-www-form-urlencoded");
                return pars;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("current", currentRFID);
                params.put("next", nextNode);
                params.put("bearing", currentBearing);
                return params;
            }

        };
        requestQueue.add(jsonObjRequest);
    }


    private void cancelAll() {
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    private void getDirectionFromDb(final String fromNode, final String toNode) {
        Log.d(TAG, "Getting direction from Db fromNode:" + fromNode + " toNode:" + toNode);
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL_PATH,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Received direction response:" + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray jsonArray = jsonObject.getJSONArray("Path");
                            SpeechServices.addText(String.format(Locale.ENGLISH, "To get from %s to %s you must go to", RfidServices.encodeIdToName(fromNode), toNode));
                            ArrayList<String> nodes = new ArrayList<>();
                            for (int jsonIter = 1; jsonIter < jsonArray.length(); jsonIter++) {
                                SpeechServices.addText(jsonArray.getString(jsonIter));
                                nodes.add(jsonArray.getString(jsonIter));
                                if (jsonIter < (jsonArray.length() - 1)) {
                                    SpeechServices.addText("then");
                                }
                            }
                            RfidServices.setNodes(nodes);
                            ((TextView) callerActivity.findViewById(R.id.di_content_path)).setText(nodes.toString());
                            BearingRequest bearingRequest = new BearingRequest(RfidServices.getCurrentLocation(), RfidServices.peekFirst(), String.valueOf(BearingListener.getBearing()));
                            RequestServices.addBearingRequest(bearingRequest);
                        } catch (JSONException e) {
                            Log.e(this.toString(), e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Log.d(this.toString(),
                        (e == null || e.getMessage() == null) ? "An unexpected error have occured" : e
                                .getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> pars = new HashMap<>();
                pars.put("Content-Type", "application/x-www-form-urlencoded");
                return pars;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("from", fromNode);
                params.put("to", toNode);
                return params;
            }

        };
        requestQueue.add(jsonObjRequest);
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            if (!bearingQueue.isEmpty()) {
                BearingRequest bearingRequest = bearingQueue.poll();
                getBearingFromDb(filterBluetooth(bearingRequest.currentRFID), bearingRequest.nextNode, bearingRequest.currentBearing);
            }
            if (!directionQueue.isEmpty()) {
                DirectionRequest directionRequest = directionQueue.poll();
                getDirectionFromDb(filterBluetooth(directionRequest.fromNode), directionRequest.toNode);
            }
        }
        cancelAll();
    }

    public static void addBearingRequest(BearingRequest bearingRequest) {
        bearingQueue.add(bearingRequest);
    }

    public static void addDirectionRequest(DirectionRequest directionRequest) {
        directionQueue.add(directionRequest);
    }
}
