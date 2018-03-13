package io.github.samirabulle.maraudersmap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    public static final String PUBNUB_PUBLISH_KEY = "pub-c-53ee49d7-c3d9-4787-b8f7-81d98202b5ff";
    public static final String PUBNUB_SUBSCRIBE_KEY = "sub-c-0cd4fdb4-2652-11e8-8f89-fe0057f68997";

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private PubNub mPubNub;

    private String mChannel;
    private String mUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        mChannel = intent.getStringExtra(LoginActivity.CHANNEL);
        mUser = intent.getStringExtra(LoginActivity.USERNAME);

        // PubNub stuff
        mPubNub = initPubNub();

        // Location Stuff
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = createLocationRequest();
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    publishLocation(locationResult.getLastLocation());
                    Log.v(TAG, location.toString());
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest request = new LocationRequest();
        request.setInterval(5000);
        request.setFastestInterval(1000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return request;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback, null /* Looper */);
    }

    @NonNull
    private PubNub initPubNub() {
        PNConfiguration pnConfiguration = new PNConfiguration()
                .setPublishKey(PUBNUB_PUBLISH_KEY)
                .setSubscribeKey(PUBNUB_SUBSCRIBE_KEY)
                .setSecure(false);
        Log.v(TAG, PUBNUB_PUBLISH_KEY);
        return new PubNub(pnConfiguration);
    }

    private void publishLocation(final Location location) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("user", mUser);
        locationMap.put("lat", location.getLatitude());
        locationMap.put("lng", location.getLongitude());

        mPubNub.publish()
                .message(locationMap)
                .channel(mChannel)
                .async(
                        new PNCallback<PNPublishResult>() {
                            @Override
                            public void onResponse(PNPublishResult result, PNStatus status) {
                                try {
                                    if (!status.isError()) {
                                        Log.v(TAG, "publish(" + result.toString() + ")");
                                    } else {
                                        Log.v(TAG, "publishErr(" + status.toString() + ")");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
    }
}
