package com.example.weatherapplication.Service;

import com.example.weatherapplication.model.WeatherModel;
import com.example.weatherapplication.util.Constants;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {

    @GET(Constants.SUB_URL)
    Call<WeatherModel> getWeatherByLocation(@Query("lat") double userLatitude ,@Query("lon") double userLongitude);

    @GET(Constants.SUB_URL)
    Call<WeatherModel> getWeatherByCityName(@Query("q") String cityName);
}
