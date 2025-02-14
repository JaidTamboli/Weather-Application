package com.example.weatherapplication.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.weatherapplication.R;
import com.example.weatherapplication.databinding.ActivityWeatherBinding;
import com.example.weatherapplication.model.WeatherModel;
import com.example.weatherapplication.util.Constants;
import com.example.weatherapplication.util.NetworkAlertDialogCreator;
import com.example.weatherapplication.util.networkutil.NetworkConnectionObserver;
import com.example.weatherapplication.util.networkutil.NetworkStatusListener;
import com.example.weatherapplication.viewmodel.WeatherViewModel;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class WeatherActivity extends AppCompatActivity implements NetworkStatusListener {

    ActivityWeatherBinding weatherBinding;
    String prefer;
    WeatherViewModel weatherViewModel;
    LocationManager locationManager;
    LocationListener locationListener;
    double lat;
    double lon;
    AlertDialog dialog;
    NetworkConnectionObserver networkConnectionObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherBinding = ActivityWeatherBinding.inflate(getLayoutInflater());
        setContentView(weatherBinding.getRoot());

        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        weatherBinding.LinearLayoutWeatherData.setVisibility(View.INVISIBLE);
        prefer = getIntent().getStringExtra(Constants.intentName);
        if (prefer != null) {
            if (prefer.equals(Constants.byLocation)) {
                checkLocationPermissionAndFetchWeather();
            } else {
                weatherBinding.progressBarWeatherData.setVisibility(View.INVISIBLE);
            }
        }

        weatherBinding.toolBar.setNavigationOnClickListener(v -> finish());
        weatherBinding.search.setOnClickListener(v -> getWeatherDataByCityName());

        weatherBinding.refreshLayout.setOnRefreshListener(() -> {
            if (prefer.equals(Constants.byLocation)) {
                checkLocationPermissionAndFetchWeather();
            } else {
                getWeatherDataByCityName();
            }
            weatherBinding.refreshLayout.setRefreshing(false);
        });
        dialog = NetworkAlertDialogCreator.createNetworkAlertDialog(this).create();
        networkConnectionObserver = new NetworkConnectionObserver(this, this);
        weatherViewModel.setNetworkConnectionObserver(networkConnectionObserver);
    }

    public void getWeatherDataByCityName() {
        String cityName = weatherBinding.editTextCityName.getText().toString();

        if (cityName.isEmpty()) {
            Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
        } else {
            weatherViewModel.getProgressBarLiveData().observe(WeatherActivity.this, progressState -> {
                if (progressState) {
                    weatherBinding.progressBarWeatherData.setVisibility(View.VISIBLE);
                    weatherBinding.LinearLayoutWeatherData.setVisibility(View.INVISIBLE);
                } else {
                    weatherBinding.progressBarWeatherData.setVisibility(View.INVISIBLE);
                }
            });

            weatherViewModel.sendRequestByCityName(getApplicationContext(), cityName);
            weatherViewModel.getWeatherResponseLiveData().observe(WeatherActivity.this, this::showWeatherData);
        }
    }

    private void checkLocationPermissionAndFetchWeather() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getWeatherDataByLocation();
        }
    }

    @SuppressLint("MissingPermission")
    public void getWeatherDataByLocation() {
        weatherBinding.linearLayoutSearch.setVisibility(View.INVISIBLE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Please enable GPS or Network Location", Toast.LENGTH_SHORT).show();
            return;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lat = location.getLatitude();
                lon = location.getLongitude();
                Log.d("Latitude", String.valueOf(lat));
                Log.d("Longitude", String.valueOf(lon));
                fetchWeatherByLocation(lat, lon);
                locationManager.removeUpdates(this);  // Stop updates after getting the location
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 50, locationListener);
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 50, locationListener);
        }

        // Fallback: Use last known location if no updates received
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation == null) {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (lastKnownLocation != null) {
            lat = lastKnownLocation.getLatitude();
            lon = lastKnownLocation.getLongitude();
            fetchWeatherByLocation(lat, lon);
        }
    }

    private void fetchWeatherByLocation(double latitude, double longitude) {
        weatherViewModel.getProgressBarLiveData().observe(this, progressState -> {
            if (progressState) {
                weatherBinding.progressBarWeatherData.setVisibility(View.VISIBLE);
                weatherBinding.LinearLayoutWeatherData.setVisibility(View.INVISIBLE);
            } else {
                weatherBinding.progressBarWeatherData.setVisibility(View.INVISIBLE);
            }
        });

        weatherViewModel.sendRequestByLocation(getApplicationContext(), latitude, longitude);
        weatherViewModel.getWeatherResponseLiveData().observe(this, this::showWeatherData);
    }

    public void showWeatherData(WeatherModel response) {
        weatherBinding.textViewCityName.setText(response.getName() + "," + response.getSys().getCountry());
        weatherBinding.textViewTemperature.setText(response.getMain().getTemp() + "°C");
        weatherBinding.textViewDescription.setText(response.getWeather().get(0).getDescription());
        weatherBinding.textViewHumidity.setText(" : " + response.getMain().getHumidity() + "%");
        weatherBinding.textViewMaxTemp.setText(" : " + response.getMain().getTemp_max() + "°C");
        weatherBinding.textViewMinTemp.setText(" : " + response.getMain().getTemp_min() + "°C");
        weatherBinding.textViewPressure.setText(" : " + response.getMain().getPressure());
        weatherBinding.textViewWind.setText(" : " + response.getWind().getSpeed());

        weatherBinding.LinearLayoutWeatherData.setVisibility(View.VISIBLE);
        weatherBinding.progressBarWeatherIcon.setVisibility(View.VISIBLE);

        String iconCode = response.getWeather().get(0).getIcon();
        Picasso.get().load("https://openweathermap.org/img/wn/" + iconCode + "@2x.png")
                .into(weatherBinding.imageViewWeatherIcon, new Callback() {
                    @Override
                    public void onSuccess() {
                        weatherBinding.progressBarWeatherIcon.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Exception e) {
                        weatherBinding.imageViewWeatherIcon.setImageResource(R.drawable.partly_cloudy_day);
                        Log.d("iconerror", e.getLocalizedMessage());
                        weatherBinding.progressBarWeatherIcon.setVisibility(View.INVISIBLE);
                    }
                });
    }

    @Override
    public void onNetworkAvailable() {
        runOnUiThread(() -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onNetworkLost() {
        runOnUiThread(() -> {
            if (dialog != null && !dialog.isShowing()) {
                dialog.show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        networkConnectionObserver.registerCallback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        networkConnectionObserver.unregisterCallback();
    }
}
