package com.location.app.locationservice;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.location.app.locationlibrary.LocationUtils;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSION_REQUEST_CODE = 10000;

    LocationUtils mLocationUtils;
    TextView mTvPosition, mTvAddress;
    ImageView mImageView;
    String mImei = "0339c1c0-8619-8d4dac724b11";

    String mVender = "ddm";
    String mBroker = "";
    String mAccessKey = "";
    String mSecretKey = "";
    String mTopic = "tracks_staging";
    String mProducerClientId = "GID_tracks_staging";

    private static String[] mPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvPosition = (TextView) findViewById(R.id.textView);
        mTvAddress = (TextView) findViewById(R.id.textView2);
        mImageView = (ImageView) findViewById(R.id.imageView);

        mTvPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        if (Edition.getSDKVersionNumber() >= 23) {
            boolean isAllGranted = checkPermissionAllGranted(mPermissions);

            if (isAllGranted) {
                startLocation();
                return;
            }

            ActivityCompat.requestPermissions(this, mPermissions, MY_PERMISSION_REQUEST_CODE);
        } else {
            startLocation();
        }
    }

    public void startLocation() {
        mLocationUtils = new LocationUtils.LocationUtilsBuilder(mImei, this)
//                可选参数
//                .withBroker(mBroker)
//                .withAccessKey(mAccessKey)
//                .withSecretKey(mSecretKey)
//                .withVender(mVender)
//                .withTopic(mTopic)
                .withProducerClientId(mProducerClientId + "@@@" + mImei)
                .build();

        mLocationUtils.startLocationService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocationUtils != null) {
            // mLocationUtils.startLocationService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationUtils.stopLocationService();
    }

    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSION_REQUEST_CODE) {
            boolean isAllGranted = true;

            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (isAllGranted) {
                startLocation();

            } else {
                openAppDetails();
            }
        }
    }

    private void openAppDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("备份通讯录需要访问 “定位”，请到 “应用信息 -> 权限” 中授予！");
        builder.setPositiveButton("去手动授权", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
