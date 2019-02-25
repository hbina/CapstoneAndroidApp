package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DirectionServices {

    private final static String SMART_STICK_URL_DIRECTION = "http://Capstone4913-env.rpwrn4wmqm.us-east-2.elasticbeanstalk.com/path/getDirection";
    private final static String SMART_STICK_URL_PATH = "http://Capstone4913-env.rpwrn4wmqm.us-east-2.elasticbeanstalk.com/path";
    private final static String TAG = "DirectionServices";
    private final RequestQueue requestQueue;
    private final TextToSpeechServices textToSpeechServices;

    public DirectionServices(MainActivity activity, TextToSpeechServices textToSpeechServices) {
        requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        this.textToSpeechServices = textToSpeechServices;
    }

    void getBearingFromDb(final String currentRFID, final String nextNode, final String currentBearing) {
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
                            textToSpeechServices.logAndSpeak(toSpeak);
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


    public void cancelAll() {
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    public void getDirectionFromDb(final String fromNode, final String toNode) {
        final ServicesTerminal servicesTerminal = ServicesTerminal.getServicesTerminal();
        servicesTerminal.setDestinationNode(toNode);
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL_PATH,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject reader = new JSONObject(response);
                            JSONArray paths = reader.getJSONArray("Path");
                            servicesTerminal.clearPaths();
                            textToSpeechServices.logAndSpeak(String.format(Locale.ENGLISH, "To get from %s to %s you must go to", fromNode, toNode));
                            for (int i = 0; i < paths.length(); i++) {
                                textToSpeechServices.logAndSpeak(paths.getString(i));
                                servicesTerminal.addNodeToPath(paths.getString(i));
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
}
