package com.location.app.locationlibrary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.core.app.utils.MqttSend;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Ting on 17/6/19.
 */

public class LocationUtils {

    private static final String TAG = "POSITION";
    public static final int SHOW_LOCATION = 0;//更新文字式的位置信息
    public static final int SHOW_LATLNG = 1; //更新经纬坐标式的位置信息
    private LocationManager mLocationManager;
    private String mProvider;
    private final String mVender;
    private final String mImei;
    private final Context mContext;
    private final String mBroker;
    private final String mAccessKey;
    private final String mSecretKey;
    private final String mTopic;
    private final String mProducerClientId;


    public LocationUtils(LocationUtilsBuilder builder) {
        this.mImei = builder.imei;
        this.mContext = builder.context;
        this.mVender = builder.vender;
        this.mBroker = builder.broker;
        this.mAccessKey = builder.accessKey;
        this.mSecretKey = builder.secretKey;
        this.mTopic = builder.topic;
        this.mProducerClientId = builder.producerClientId;
    }

    private Handler handler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            double currentLatitude = bundle.getDouble("latitude");
            double currentLongitude = bundle.getDouble("longitude");
            double currentAltitude = bundle.getDouble("altitude");
            double currentSpeed = bundle.getDouble("speed");
            long currentTime = bundle.getLong("time");
            Log.d(TAG, "showLocation:" + currentLatitude);
            Log.d(TAG, "showLocation:" + currentLongitude);
            Log.d(TAG, "showLocation:" + currentAltitude);
            Log.d(TAG, "showLocation:" + currentSpeed);
            Log.d(TAG, "showLocation:" + currentTime);

            JSONObject detailJson = new JSONObject();
            try {
                detailJson.put("imei", mImei);
                detailJson.put("vender", mVender);
                detailJson.put("lon", currentLongitude);
                detailJson.put("lat", currentLatitude);
                detailJson.put("alt", currentAltitude);
                detailJson.put("v", currentSpeed);
                detailJson.put("date", TimeUtils.parseDate(currentTime));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            MqttSend mqttSend = new MqttSend(mBroker, mAccessKey, mSecretKey);
            mqttSend.sendMessage(mTopic, mProducerClientId, detailJson.toString());

        }
    };

    public void startLocationService() {
        Location location = getLastKnownLocation();
        if (location != null) {
            updateLocation(location);
        }

        updateLocationService();
    }

    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            Location l = mLocationManager.getLastKnownLocation(provider);
            mProvider = provider;
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public void updateLocationService() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(mProvider, 2000, 5, locationListener);
    }

    public void stopLocationService() {
        if (mLocationManager != null) {
            // 关闭程序时将监听器移除
            mLocationManager.removeUpdates(locationListener);
        }
    }

    //LocationListener 用于当位置信息变化时由 locationManager 调用
    LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

    };

    private void updateLocation(final Location location) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putDouble("longitude", location.getLongitude());
                bundle.putDouble("latitude", location.getLatitude());
                bundle.putDouble("altitude", location.getAltitude());
                bundle.putDouble("speed", location.getSpeed());
                bundle.putLong("time", location.getTime());
                msg.setData(bundle);
                handler.sendMessage(msg);
            }

        }).start();
    }

    private void parseJSONResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            //获取results节点下的位置
            JSONArray resultArray = jsonObject.getJSONArray("results");
            if (resultArray.length() > 0) {
                JSONObject subObject = resultArray.getJSONObject(0);
                //取出格式化后的位置信息
                String address = subObject.getString("formatted_address");
                Message message = new Message();
                message.what = SHOW_LOCATION;
                message.obj = address;
                handler.sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class LocationUtilsBuilder {

        private final String imei;
        private final Context context;

        private String vender = "ddm";
        private String broker = "";
        private String accessKey = "";
        private String secretKey = "";
        private String topic = "tracks-staging";
        private String producerClientId = "GID_track_staging";

        public LocationUtilsBuilder(String imei, Context context) {
            this.imei = imei;
            this.context = context;
        }

        public LocationUtilsBuilder withVender(String vender) {
            this.vender = vender;
            return this;
        }

        public LocationUtilsBuilder withBroker(String broker) {
            this.broker = broker;
            return this;
        }

        public LocationUtilsBuilder withAccessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        public LocationUtilsBuilder withSecretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        public LocationUtilsBuilder withTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public LocationUtilsBuilder withProducerClientId(String producerClientId) {
            this.producerClientId = producerClientId;
            return this;
        }

        public LocationUtils build() {
            return new LocationUtils(this);
        }
    }
}
