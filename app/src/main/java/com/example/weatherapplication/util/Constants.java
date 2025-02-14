package com.example.weatherapplication.util;

import android.Manifest;

public class Constants {

    public static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final String nameOfSharedPreferences = "com.example.weatherapplication";

    public static final String keyForDeniedAllPermissionsCount = "deniedAllPermissionsCount";
    public static final String keyForDeniedOnlyFinePermissionCount = "deniedOnlyFinePermissionCount";
    public static final String intentName = "weather";
    public static final String byCityName = "by city name";
    public static final String byLocation = "by location";
    public static final String BASE_URL = "https://api.openweathermap.org/";
    public static final String SUB_URL = "/data/2.5/weather?&appid=a20ae16ac958de59fc8a9045193321af&units=metric";
}
