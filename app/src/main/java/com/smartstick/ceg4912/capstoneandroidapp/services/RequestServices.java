package com.smartstick.ceg4912.capstoneandroidapp.services;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;
import com.smartstick.ceg4912.capstoneandroidapp.model.BearingRequest;
import com.smartstick.ceg4912.capstoneandroidapp.model.DirectionRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RequestServices extends Services {

    private final static String SMART_STICK_URL_DIRECTION = "http://Capstone4913-env.rpwrn4wmqm.us-east-2.elasticbeanstalk.com/path/getDirection";
    private final static String SMART_STICK_URL_PATH = "http://Capstone4913-env.rpwrn4wmqm.us-east-2.elasticbeanstalk.com/path";
    private final static String TAG = "RequestServices";

    private final static ConcurrentLinkedQueue<BearingRequest> bearingQueue = new ConcurrentLinkedQueue<>();
    private final static ConcurrentLinkedQueue<DirectionRequest> directionQueue = new ConcurrentLinkedQueue<>();

    private final RequestQueue requestQueue;

    public RequestServices(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    private void getBearingFromDb(final String currentRFID, final String nextNode, final String currentBearing) {
        Log.d(this.toString(), String.format("currentRFID:%s nextNode:%s\n", currentRFID, nextNode));
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL_DIRECTION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject reader = (new JSONObject(response)).getJSONObject("Navigation");

                            String direction = reader.getString("direction");
                            int bearing = reader.getInt("bearingDestination");

                            String toSpeak = String.format(Locale.ENGLISH, "turn %d to get to %s", bearing, direction);
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
                params.put("currentBearing", currentBearing);
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
                        try {
                            JSONObject reader = new JSONObject(response);
                            JSONArray paths = reader.getJSONArray("Path");
                            SpeechServices.addText(String.format(Locale.ENGLISH, "To get from %s to %s you must go to", fromNode, toNode));
                            for (int i = paths.length() - 1; i > 0; i--) {
                                SpeechServices.addText(paths.getString(i));
                                // TODO: Add paths somewhere...preferably DirectionServices...
                            }
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
        super.run();
        while (isRunning.get()) {
            if (!bearingQueue.isEmpty()) {
                BearingRequest bearingRequest = bearingQueue.poll();
                getBearingFromDb(bearingRequest.currentRFID, bearingRequest.nextNode, bearingRequest.currentBearing);
            }
            if (!directionQueue.isEmpty()) {
                DirectionRequest directionRequest = directionQueue.poll();
                getDirectionFromDb(directionRequest.fromNode, directionRequest.toNode);
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
