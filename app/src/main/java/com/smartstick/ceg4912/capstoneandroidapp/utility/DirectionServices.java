package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.app.Activity;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.smartstick.ceg4912.capstoneandroidapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DirectionServices {

    private final static String SMART_STICK_URL = "http://Capstone4913-env.rpwrn4wmqm.us-east-2.elasticbeanstalk.com/path/getDirection";
    private static RequestQueue requestQueue;

    public DirectionServices(Activity activity) {
        requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
    }

    public ArrayList<String> getDirectionFromDb(final String from, final String to) {
        Log.d(this.toString(), String.format("from:%s to:%s\n", from, to));
        final ArrayList<String> paths = new ArrayList<>();
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject reader = (new JSONObject(response)).getJSONObject("Navigation");
                            String direction = reader.getString("direction");
                            int bearing = reader.getInt("bearingDestination");
                            Log.d(this.toString(), String.format("direction%s bearing%d", direction, bearing));
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
        return paths;
    }


    public void cancelAll() {
        if (requestQueue != null) {
            requestQueue.cancelAll(this.toString());
        }
    }
}
