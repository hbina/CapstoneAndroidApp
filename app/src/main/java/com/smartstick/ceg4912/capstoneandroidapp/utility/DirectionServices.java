package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.app.Activity;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;
import com.smartstick.ceg4912.capstoneandroidapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DirectionServices {

    private final static String SMART_STICK_URL = "http://Capstone4913-env.rpwrn4wmqm.us-east-2.elasticbeanstalk.com/path/getDirection";
    private RequestQueue requestQueue;
    private MainActivity callerActivity;

    public DirectionServices(MainActivity activity) {
        requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        callerActivity = activity;
    }

    public void getDirectionFromDb(final String from, final String to, final TextToSpeechServices textToSpeechServices) {
        Log.d(this.toString(), String.format("from:%s to:%s\n", from, to));
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject reader = (new JSONObject(response)).getJSONObject("Navigation");
                            String direction = reader.getString("direction");
                            int bearing = reader.getInt("bearingDestination");
                            textToSpeechServices.logAndSpeak(String.format(Locale.ENGLISH, "turn %d to get to %s", bearing, direction));
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
                params.put("from", from);
                params.put("to", to);
                return params;
            }

        };
        requestQueue.add(jsonObjRequest);
    }


    public void cancelAll() {
        if (requestQueue != null) {
            requestQueue.cancelAll(this.toString());
        }
    }
}
