package com.example.trackfield.data.network;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trackfield.BuildConfig;
import com.example.trackfield.R;
import com.example.trackfield.data.db.DbReader;
import com.example.trackfield.data.db.DbWriter;
import com.example.trackfield.data.db.model.Exercise;
import com.example.trackfield.data.prefs.Prefs;
import com.example.trackfield.ui.exercise.ViewActivity;
import com.example.trackfield.ui.main.MainActivity;
import com.example.trackfield.ui.map.model.Trail;
import com.example.trackfield.utils.AppConsts;
import com.example.trackfield.utils.DateUtils;
import com.example.trackfield.utils.LayoutUtils;
import com.example.trackfield.utils.model.PairList;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class StravaApi {

    // token response json keys
    private static final String JSON_ACCESS_TOKEN = "access_token";
    private static final String JSON_REFRESH_TOKEN = "refresh_token";
    private static final String JSON_EXPIRES_AT = "expires_at";

    // activity response json keys
    private static final String JSON_ID = "id";
    private static final String JSON_NAME = "name";
    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_DISTANCE = "distance";
    private static final String JSON_TIME = "elapsed_time";
    private static final String JSON_TYPE = "type";
    private static final String JSON_DATE = "start_date_local";
    private static final String JSON_MAP = "map";
    private static final String JSON_POLYLINE = "summary_polyline";
    private static final String JSON_START = "start_latlng";
    private static final String JSON_END = "end_latlng";
    private static final String JSON_DEVICE = "device_name";

    // api values
    private static final String CLIENT_ID = BuildConfig.STRAVA_CLIENT_ID;
    private static final String CLIENT_SECRET = BuildConfig.STRAVA_CLIENT_SECRET;
    private static final String REDIRECT_URI = "https://felwal.github.io/callback";
    private static final int PER_PAGE = 200; // max = 200

    private static final String LOG_TAG = "StravaAPI";
    private static final DateTimeFormatter FORMATTER_STRAVA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static RequestQueue queue;

    private final Activity a;

    //

    public StravaApi(Activity a) {
        this.a = a;
        queue = Volley.newRequestQueue(a);
    }

    // authorize

    public void authorizeStrava() {
        Uri uri = Uri.parse("https://www.strava.com/oauth/mobile/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:read_all")
            .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        a.startActivity(intent);
    }

    /**
     * Handles intent for strava authorization result; checks if intent contains any URI appLinkData and finishes
     * authorization.
     *
     * @param appLinkIntent The intent possibly containing appLinkData
     */
    public void handleIntent(Intent appLinkIntent) {
        Uri appLinkData = appLinkIntent.getData();
        if (appLinkData != null) finishAuthorization(appLinkData);
    }

    private void finishAuthorization(Uri appLinkData) {
        String authCode = appLinkData.getQueryParameter("code");
        if (authCode != null) {
            Prefs.setAuthCode(authCode);
            LayoutUtils.toast(R.string.toast_strava_auth_successful, a);
        }
        else {
            LayoutUtils.toast(R.string.toast_strava_auth_err, a);
        }
    }

    // pull activities

    public void pullActivity(final long stravaId, ResponseListener listener) {
        ((TokenRequester) accessToken -> {
            LayoutUtils.toast("Pulling activity...", a);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, getActivityURL(stravaId), null,
                response -> {
                    Log.i(LOG_TAG, "response: " + response.toString());

                    boolean success = handlePull(convertToExercise(response));

                    listener.onStravaResponse(success);
                }, e -> listener.onStravaResponseError(e, a));

            queue.add(request);
        }).requestAccessToken(a);
    }

    public void pullAllActivities(ResponseListener listener) {
        ArrayList<Long> stravaIds = DbReader.get(a).getExternalIds();
        if (stravaIds.size() == 0) return;

        ((TokenRequester) accessToken -> {
            LayoutUtils.toast("Pulling activities...", a);

            for (long stravaId : stravaIds) {
                JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, getActivityURL(stravaId), null,
                    response -> {
                        Log.i(LOG_TAG, "response: " + response.toString());

                        boolean success = handlePull(convertToExercise(response));

                        listener.onStravaResponse(success);
                    }, e -> listener.onStravaResponseError(e, a));

                queue.add(request);
            }
        }).requestAccessToken(a);
    }

    // request activities

    private void requestActivity(final int index, ResponseListener listener) {
        ((TokenRequester) accessToken -> {
            LayoutUtils.toast("Requesting activity...", a);

            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, getActivitiesURL(1), null, response -> {
                Log.i(LOG_TAG, "response: " + response);

                try {
                    JSONObject obj = response.getJSONObject(index);
                    Exercise requested = convertToExercise(obj);
                    boolean success = handleRequest(requested);

                    Log.i(LOG_TAG, "response obj: " + obj.toString());

                    listener.onStravaResponse(success);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    LayoutUtils.handleError(R.string.toast_err_parse_jsonobj, e, a);
                }
            }, e -> listener.onStravaResponseError(e, a));

            queue.add(request);
        }).requestAccessToken(a);
    }

    private void requestActivities(final int page, ResponseListener listener) {
        ((TokenRequester) accessToken -> {
            LayoutUtils.toast("Requesting activities...", a);

            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, getActivitiesURL(page), null,
                response -> {
                    Log.i(LOG_TAG, "response: " + response);

                    int errorCount = 0;
                    for (int index = 0; index < response.length(); index++) {
                        boolean success = true;
                        try {
                            JSONObject obj = response.getJSONObject(index);
                            Exercise requested = convertToExercise(obj);
                            success &= handleRequest(requested);
                        }
                        catch (JSONException e) {
                            success = false;
                            e.printStackTrace();
                            LayoutUtils.handleError(R.string.toast_err_parse_jsonobj, e, a);
                        }
                        errorCount += success ? 0 : 1;
                    }

                    // request next page
                    if (response.length() == PER_PAGE) {
                        requestActivities(page + 1, listener);
                    }

                    listener.onStravaResponse(errorCount == 0);
                }, e -> listener.onStravaResponseError(e, a));

            queue.add(request);
        }).requestAccessToken(a);
    }

    // request activities: helper methods

    public void requestLastActivity(ResponseListener listener) {
        requestActivity(0, listener);
    }

    public void requestLastActivities(int count, ResponseListener listener) {
        for (int i = 0; i < count; i++) {
            requestActivity(i, listener);
        }
    }

    public void requestAllActivities(ResponseListener listener) {
        requestActivities(1, listener);
    }

    // convert

    private Exercise convertToExercise(JSONObject obj) {
        if (obj == null) return null;

        try {
            // keys which always exist
            Log.i(LOG_TAG, "response: " + obj.toString());
            long stravaId = obj.getLong(JSON_ID);
            String name = obj.getString(JSON_NAME);
            int distance = (int) obj.getDouble(JSON_DISTANCE);
            int time = obj.getInt(JSON_TIME);
            String stravaType = obj.getString(JSON_TYPE);
            String date = obj.getString(JSON_DATE);
            String method = Prefs.getRecordingMethod();

            // pull keys
            String description = obj.has(JSON_DESCRIPTION) ? obj.getString(JSON_DESCRIPTION) : "";
            String device = obj.has(JSON_DEVICE) ? obj.getString(JSON_DEVICE) : "";

            // trail
            String polyline = null;
            LatLng start = null;
            LatLng end = null;
            try {
                JSONObject map = obj.getJSONObject(JSON_MAP);
                polyline = map.getString(JSON_POLYLINE);
                JSONArray startLatLng = obj.getJSONArray(JSON_START);
                JSONArray endLatLng = obj.getJSONArray(JSON_END);
                start = new LatLng(startLatLng.getDouble(0), startLatLng.getDouble(1));
                end = new LatLng(endLatLng.getDouble(0), endLatLng.getDouble(1));
            }
            catch (Exception e) {
                // no polyline or start or end, leave as null; do nothing
            }

            // convert
            int type = convertType(stravaType);
            int routeId = DbReader.get(a).getRouteIdOrCreate(name, a);
            LocalDateTime dateTime = LocalDateTime.parse(date, FORMATTER_STRAVA);
            Trail trail = polyline == null || polyline.equals("null") || polyline.equals("") ? null :
                new Trail(polyline, start, end);

            return new Exercise(Exercise.NO_ID, stravaId, type, dateTime, routeId, name, "", "", description, device,
                method, distance, time, null, trail);
        }
        catch (JSONException e) {
            e.printStackTrace();
            LayoutUtils.handleError("Failed parse JSONObject, returning null", e, a);
            return null;
        }
    }

    private boolean handlePull(Exercise strava) {
        if (strava == null) return false;
        boolean success = true;

        Exercise existing = DbReader.get(a).getExercise(strava.getExternalId());

        // import
        if (existing == null) {
            success &= DbWriter.get(a).addExercise(strava, a);
            LayoutUtils.toast("Pull resulted in import on " + strava.getDate().format(AppConsts.FORMATTER_SQL_DATE), a);
            Log.i(LOG_TAG, "Pull resulted in import on " + strava.getDate().format(AppConsts.FORMATTER_SQL_DATE));
        }

        // merge
        else {
            PairList<String, Boolean> settings = Prefs.getPullSettings();

            if (settings.getSecond("Route")) {
                existing.setRoute(strava.getRoute());
                existing.setRouteId(strava.getRouteId());
            }
            if (settings.getSecond("Type")) {
                existing.setType(strava.getType());
            }
            if (settings.getSecond("Date and time")) {
                existing.setDateTime(strava.getDateTime());
            }
            if (settings.getSecond("Data source")) {
                existing.setDataSource(strava.getDataSource());
            }
            if (settings.getSecond("Distance")) {
                existing.setDistance(strava.getDistance());
            }
            if (settings.getSecond("Time")) {
                existing.setTime(strava.getTime());
            }
            if (settings.getSecond("Note") && !strava.getNote().equals("")) {
                existing.setNote(strava.getNote());
            }
            if (settings.getSecond("Trail")) {
                existing.setTrail(strava.getTrail());
            }

            success &= DbWriter.get(a).updateExercise(existing, a);
        }

        if (a instanceof ViewActivity) a.recreate();

        return success;
    }

    private boolean handleRequest(Exercise strava) {
        if (strava == null) return false;
        boolean success = true;

        // dont override already existing (use pull for that)
        Exercise existing = DbReader.get(a).getExercise(strava.getExternalId());
        if (existing != null) return true;

        // merge with matching, ie not already linked to strava activity
        ArrayList<Exercise> matching = DbReader.get(a).getExercises(strava.getDateTime());

        // merge
        if (matching.size() == 1) {
            Exercise x = matching.get(0);
            Exercise merged = new Exercise(x.getId(), strava.getExternalId(), x.getType(), strava.getDateTime(),
                x.getRouteId(), x.getRoute(), x.getRouteVar(), x.getInterval(), x.getNote(), x.getDataSource(),
                x.getRecordingMethod(), strava.getDistance(), strava.getTime(), x.getSubs(),
                strava.getTrail());

            success &= DbWriter.get(a).updateExercise(merged, a);
        }

        // import
        else if (matching.size() == 0) {
            success &= DbWriter.get(a).addExercise(strava, a);
            Log.i(LOG_TAG, "Import on " + strava.getDate().format(AppConsts.FORMATTER_SQL_DATE));
            //L.toast("Import on " + fromStrava.getDate().format(C.FORMATTER_SQL_DATE), a);
        }

        // nothing
        else {
            success = false;
            Log.i(LOG_TAG, "Multiple choice on " + strava.getDateTime().format(AppConsts.FORMATTER_SQL_DATE));
            //L.toast("Multiple choice on " + fromStrava.getDateTime().format(C.FORMATTER_SQL_DATE), a);
        }

        if (a instanceof MainActivity) ((MainActivity) a).updateFragment();

        // also pull to get data not available to request
        pullActivity(strava.getExternalId(), responseSuccess -> {});

        return success;
    }

    // get url:s

    private static String getRefreshTokenURL() {
        return "https://www.strava.com/oauth/token?client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET +
            "&code=" + Prefs.getAuthCode() + "&grant_type=authorization_code";
    }

    private static String getAccessTokenURL() {
        return "https://www.strava.com/oauth/token?client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET +
            "&refresh_token=" + Prefs.getRefreshToken() + "&grant_type=refresh_token";
    }

    private static String getActivitiesURL(int page) {
        return "https://www.strava.com/api/v3/athlete/activities?per_page=" + PER_PAGE + "&access_token=" +
            Prefs.getAccessToken() + "&page=" + page;
    }

    private static String getActivityURL(long id) {
        return "https://www.strava.com/api/v3/activities/" + id + "?include_all_efforts=false" + "&access_token=" +
            Prefs.getAccessToken();
    }

    // launch

    public static void launchStravaActivity(long stravaId, Activity a) {
        Uri uri = Uri.parse("https://www.strava.com/activities/" + stravaId).buildUpon().build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        a.startActivity(intent);
    }

    // tools

    private static int convertType(String stravaType) {
        switch (stravaType) {
            case "Run": return Exercise.TYPE_RUN;
            case "Walk":
            case "Hike": return Exercise.TYPE_WALK;
            case "Ride": return Exercise.TYPE_RIDE;
            case "Swim":
            case "Apline Ski":
            case "Backcountry Ski":
            case "Canoe":
            case "Crossfit": return Exercise.TYPE_STRENGTH;
            case "E-Bike Ride":
            case "Elliptical":
            case "Handcycle":
            case "Ice Skate":
            case "Inline Skate":
            case "Kayak":
            case "Kitesurf Session":
            case "Nordic Ski":
            case "Row":
            case "Snowboard":
            case "Snowshoe":
            case "Stair Stepper":
            case "Stand Up Paddle":
            case "Surf":
            case "Virtual Ride":
            case "Virtual Run":
            case "Weight Training":
            case "Windsurf Session":
            case "Wheelchair":
            case "Workout":
            case "Yoga": return Exercise.TYPE_YOGA;
            default: return Exercise.TYPE_OTHER;
        }
    }

    // interface

    public interface ResponseListener {

        void onStravaResponse(boolean success);

        default void onStravaResponseError(Exception e, Context c) {
            LayoutUtils.handleError(R.string.toast_strava_response_err, e, c);
        };

    }

    private interface TokenRequester {

        void onTokenReady(String token);

        default void requestAccessToken(Context c) {
            if (Prefs.isAccessTokenCurrent()) {
                onTokenReady(Prefs.getAccessToken());
                return;
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, getAccessTokenURL(), null,
                response -> {
                    try {
                        Prefs.setAccessToken(response.getString(JSON_ACCESS_TOKEN));
                        Prefs
                            .setAccessTokenExpiration(
                                DateUtils.ofEpochSecond(Integer.parseInt(response.getString(JSON_EXPIRES_AT))));

                        //L.toast("accessToken: " + Prefs.getAccessToken(), c);
                        Log.i(LOG_TAG, "response accessToken: " + Prefs.getAccessToken());
                        onTokenReady(Prefs.getAccessToken());
                    }
                    catch (JSONException e) {
                        //e.printStackTrace();
                        LayoutUtils.handleError("Failed to parse accessToken from Strava", e, c);
                    }
                }, e -> {
                LayoutUtils.handleError(R.string.toast_strava_req_access_err, e, c);

                // request refreshToken
                ((TokenRequester) refreshToken -> ((TokenRequester) this).requestAccessToken(c))
                    .requestRefreshToken(true, c);
            });

            queue.add(request);
        }

        default void requestRefreshToken(boolean requireRequest, Context c) {
            if (!requireRequest && Prefs.isRefreshTokenCurrent()) {
                onTokenReady(Prefs.getRefreshToken());
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, getRefreshTokenURL(), null,
                response -> {
                    try {
                        Prefs.setRefreshToken(response.getString(JSON_REFRESH_TOKEN));

                        LayoutUtils.toast(R.string.toast_strava_req_refresh_successful, c);
                        Log.i(LOG_TAG, "response refreshToken: " + Prefs.getRefreshToken());
                        onTokenReady(Prefs.getRefreshToken());
                    }
                    catch (JSONException e) {
                        //e.printStackTrace();
                        LayoutUtils.handleError("Failed to parse refreshToken", e, c);
                    }
                }, e -> LayoutUtils.handleError(R.string.toast_strava_req_refresh_err, e, c)); // TODO: auto-prompt

            queue.add(request);
        }

    }

}
