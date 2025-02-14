package com.example.weatherapplication.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.weatherapplication.databinding.ActivityMainBinding;
import com.example.weatherapplication.databinding.BottomSheetDialogBinding;
import com.example.weatherapplication.util.Constants;
import com.example.weatherapplication.util.NetworkAlertDialogCreator;
import com.example.weatherapplication.util.networkutil.NetworkConnectionObserver;
import com.example.weatherapplication.util.networkutil.NetworkStatusListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MainActivity extends AppCompatActivity implements NetworkStatusListener {

    ActivityMainBinding mainBinding;
    ActivityResultLauncher<String[]> permissionsResultLauncher;
    BottomSheetDialogBinding bottomSheetDialogBinding;
    int deniedAllPermissionsCount;
    int deniedOnlyFinePermissionCount;
    SharedPreferences sharedPreferences;
    AlertDialog dialog;
    NetworkConnectionObserver networkConnectionObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        sharedPreferences = this.getSharedPreferences(Constants.nameOfSharedPreferences ,Context.MODE_PRIVATE);
        deniedAllPermissionsCount = sharedPreferences.getInt(Constants.keyForDeniedAllPermissionsCount,0);
        deniedOnlyFinePermissionCount = sharedPreferences.getInt(Constants.keyForDeniedOnlyFinePermissionCount,0);

        //Register
        registerForPermission();

        mainBinding.ButtonWeatherByCityName.setOnClickListener(v -> {
         //open the second activity
            openWeatherActivity(Constants.byCityName);

        });

        mainBinding.ButtonWeatherByLocation.setOnClickListener(v -> {
            // ask the permission of location , open the second activity
            if (hasFineLocationPermission()){
                //check location, get weather data
                checkLocationSettings();
            } else if (hasCoarseLocationPermission()) {
                saveDeniedOnlyFinePermissionCount();
                if (deniedOnlyFinePermissionCount > 2){
                    checkLocationSettings();
                }else {
                    //bottom sheet dialog for precise location
                    showBottomSheetDialog("Give precise location permission for better results ","fine","permission");
                }


            } else {
                //launch the permissionResultLauncher to request permission dialog
                permissionsResultLauncher.launch(new String[]{Constants.FINE_LOCATION,Constants.COARSE_LOCATION});
            }
        });

        dialog = NetworkAlertDialogCreator.createNetworkAlertDialog(this).create();
        networkConnectionObserver = new NetworkConnectionObserver(this,this);
    }

    public void registerForPermission(){
        permissionsResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),result -> {

            Boolean b1 = result.get(Constants.FINE_LOCATION);
            Boolean b2 = result.get(Constants.COARSE_LOCATION);

            if (b1 != null && b2 != null){
                boolean isFineLocationGranted = b1;
                boolean isCoarseLocationGranted = b2;

                if (isFineLocationGranted){
                    //check location, get weather data
                    checkLocationSettings();
                } else if(isCoarseLocationGranted){
                    saveDeniedOnlyFinePermissionCount();
                    //bottom sheet dialog for precise location
                    showBottomSheetDialog("Give precise location permission for better results ","fine","permission");
                } else {
                    deniedAllPermissionsCount++;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(Constants.keyForDeniedAllPermissionsCount,0);
                    editor.apply();
                    //bottom sheet dialog for permission
                    showBottomSheetDialog("To get the weather by location, you need enable location permission","all","permission");
                }
            }
        });
    }

    private boolean hasFineLocationPermission(){
        return ContextCompat.checkSelfPermission(this,Constants.FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean hasCoarseLocationPermission(){
        return ContextCompat.checkSelfPermission(this,Constants.COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    public void showBottomSheetDialog(String message , String deniedPermission, String useFor){

        bottomSheetDialogBinding = BottomSheetDialogBinding.inflate(getLayoutInflater());
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
        bottomSheetDialog.setContentView(bottomSheetDialogBinding.getRoot());

        if(useFor.equals("Location")){
            bottomSheetDialogBinding.buttonAllow.setText("Go");
            bottomSheetDialogBinding.textViewTitle.setText("Location");
            bottomSheetDialogBinding.textViewMessage.setText(message);
        }else {
            if (deniedAllPermissionsCount > 2 || deniedOnlyFinePermissionCount > 2){
                bottomSheetDialogBinding.buttonAllow.setText("Open");
                bottomSheetDialogBinding.textViewMessage.setText("Open the app settings to give the precise location permission.");
            }else {
                bottomSheetDialogBinding.textViewMessage.setText(message);
            }
        }

        bottomSheetDialogBinding.buttonAllow.setOnClickListener(v -> {

            if (useFor.equals("Location")){
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            } else {

                if (deniedAllPermissionsCount > 2 || deniedOnlyFinePermissionCount > 2){
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package",getPackageName(),null);//package : com.example.weatherapplication#
                    intent.setData(uri);
                    startActivity(intent);
                }
                //launch the permissionResultLauncher to request permission dialog
                permissionsResultLauncher.launch(new String[]{Constants.FINE_LOCATION,Constants.COARSE_LOCATION});
            }
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialogBinding.buttonDenny.setOnClickListener(v -> {

            if (deniedPermission.equals("fine")){
                checkLocationSettings();
            }
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    public void checkLocationSettings(){

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){

            //Open second activity
            openWeatherActivity(Constants.byLocation);
            //Toast.makeText(this, "Second Activity", Toast.LENGTH_SHORT).show();
        } else {
            showBottomSheetDialog("Go to the location settings to turn on the location",null ,"location");
        }
    }

    public void saveDeniedOnlyFinePermissionCount(){
        deniedOnlyFinePermissionCount++;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.keyForDeniedOnlyFinePermissionCount,0);
        editor.apply();
    }

    public void openWeatherActivity(String prefer){
        Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
        intent.putExtra(Constants.intentName,prefer);
        startActivity(intent);
    }

    @Override
    public void onNetworkAvailable() {

        this.runOnUiThread(() ->{
            if (dialog != null && dialog.isShowing()){
                dialog.dismiss();
            }
        });

    }

    @Override
    public void onNetworkLost() {

        this.runOnUiThread(() ->{
            if (dialog != null && dialog.isShowing()){
                dialog.show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        networkConnectionObserver.registerCallback();
        networkConnectionObserver.checkNetworkConnection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        networkConnectionObserver.unregisterCallback();
    }
}