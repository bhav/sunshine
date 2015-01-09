package com.bpadhiar.sunshine;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bpadhiar on 28/10/14.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> weatherItemAdapter;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Allows this fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart(){
        super.onStart();
        updateWeather();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        List<String> dummyWeather = new ArrayList();
        dummyWeather.add("Today - Sunny - 88/63");
        dummyWeather.add("Tomorrow - Overcast - 188/163");
        dummyWeather.add("Wednesday - Cloudy - 200/400");
        dummyWeather.add("Thursday - Stormy - 0/0");
        dummyWeather.add("Friday - Snowy - -20/-40");
        dummyWeather.add("Saturday - Icy - -40/-60");
        dummyWeather.add("Sunday - Rain - 40/60");

        weatherItemAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, dummyWeather);
        listView.setAdapter(weatherItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String itemWeather = weatherItemAdapter.getItem(position);
                Toast.makeText(getActivity().getApplicationContext(), itemWeather,
                        Toast.LENGTH_LONG).show();

                Intent toDetailIntent = new Intent(getActivity().getApplicationContext(), DetailActivity.class);
                toDetailIntent.putExtra(Intent.EXTRA_TEXT, itemWeather);
                startActivity(toDetailIntent);

            }
        });
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWeather() {
        String location = Utility.getPreferredLocation(getActivity());
        new FetchWeatherTask(weatherItemAdapter, getActivity()).execute(location);

    }






}
