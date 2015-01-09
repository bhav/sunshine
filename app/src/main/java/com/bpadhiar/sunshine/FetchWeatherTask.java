package com.bpadhiar.sunshine;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by bpadhiar on 09/01/2015.
 */
public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private final String FORECAST_BASE = "http://api.openweathermap.org/data/2.5/forecast/daily?";
    private final String QUERY = "q";
    private final String MODE = "mode";
    private final String UNITS = "metric";
    private final String DAYS = "cnt";

    private final String JSON = "json";
    private final String METRIC = "metric";
    private int numDays = 14;
    private final String[] EMPTY_RESULT = new String[]{};

    private ArrayAdapter<String> weatherItemAdapter;
    private FragmentActivity activity;



    public FetchWeatherTask(ArrayAdapter<String> weatherItemAdapter, FragmentActivity activity) {
        this.activity = activity;
        this.weatherItemAdapter = weatherItemAdapter;
    }

    @Override
    protected String[] doInBackground(String... queryLocation) {
        String locationQuery = queryLocation[0];
        if (queryLocation[0] == null) {
            return EMPTY_RESULT;
        }
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.

        String forecastJsonStr = null;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            Uri uriBuilder = Uri.parse(FORECAST_BASE).buildUpon()
                    .appendQueryParameter(QUERY, queryLocation[0])
                    .appendQueryParameter(MODE, JSON)
                    .appendQueryParameter(UNITS, METRIC)
                    .appendQueryParameter(DAYS, Integer.toString(numDays))
                    .build();


            // Create the request to OpenWeatherMap, and open the connection
            URL url = new URL(uriBuilder.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return EMPTY_RESULT;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return EMPTY_RESULT;
            }
            forecastJsonStr = buffer.toString();
            Log.d(LOG_TAG, "Forecast JSON Response String: " + forecastJsonStr);
            return getWeatherDataFromJson(forecastJsonStr, numDays, locationQuery);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
            return EMPTY_RESULT;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error ", e);
            return EMPTY_RESULT;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays, String locationQuery)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DATETIME = "dt";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime = dayForecast.getLong(OWM_DATETIME);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
            Log.v(LOG_TAG, resultStrs[i]);
        }

        return resultStrs;
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
* so for convenience we're breaking it out into its own method now.
*/
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String unitType = sharedPreferences.getString(activity.getString(R.string.pref_units_key), activity.getString(R.string.pref_units_metric));

        if (unitType.equals(activity.getString(R.string.pref_units_imperial))) {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;

        } else if (!unitType.equals(activity.getString(R.string.pref_units_metric))) {
            Log.d(LOG_TAG, "Unit Type not found: " + unitType );
        }

        String highLow = Math.round(high) + "/" + Math.round(low);
        return highLow;
    }

    @Override
    protected void onPostExecute(String[] results) {
        if (results != null) {
            weatherItemAdapter.clear();
            for (String dayWeatherResult : results) {
                weatherItemAdapter.add(dayWeatherResult);
            }
        }
    }
}