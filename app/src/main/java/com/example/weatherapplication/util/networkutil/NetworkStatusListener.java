package com.example.weatherapplication.util.networkutil;

public interface NetworkStatusListener {

    void onNetworkAvailable();
    void onNetworkLost();

}
