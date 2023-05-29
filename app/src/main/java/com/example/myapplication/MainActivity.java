package com.example.myapplication;

import static android.content.ContentValues.TAG;

import static com.example.myapplication.CS.RecordTime;
import static com.example.myapplication.ServerTask.TransTime;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class MainActivity<mThread> extends AppCompatActivity {
    private Button play;
    private CS mcs;
    private Handler handler=new Handler();
    private String[] permissions = {android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE};
    private List<String> mPermissionList = new ArrayList<>();
    private boolean flag = false;
    private ServerTask st;

//    Thread t = new Thread(new MyThread());



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
//        antiAudioWaveView = findViewById(R.id.antiAudioWaveView);
        checkPermission();
        play = findViewById(R.id.play);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!flag) {
                    play.setText("STOP");
                    if (mcs == null) {
                        mcs = new CS();
                        mcs.mTargetFrequencyDetected = false;
                        new Thread(() -> {
                            try {
                                mcs.startRecording();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }

                    if (st == null || st.isCancelled()) {
                        st = new ServerTask();
                        st.startTask();
                    }
                    flag = true;
                } else {
                    play.setText("RECORD");
                    if (mcs != null) {
                        mcs.stopRecording();
                        mcs = null;
                    }

                    if (st != null) {
                        st.stopTask();
                    }
                    flag = false;
                }
            }
        });

//        play.setOnClickListener(view -> {
//            if (!flag) {
//
//                play.setText("STOP");
//                if (st == null || st.isCancelled()) {
//                    st = new ServerTask();
//                    st.startTask();
//                    st.execute();
//                }else {
//                    st.setIsCancelled(false); // 重置标志位
//                }
//                try {
//                    mcs.startRecording();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                flag = true;
//
//
//            } else {
//                    play.setText("PLAY");
//                if (st != null && !st.isCancelled() && st.getStatus() == AsyncTask.Status.RUNNING) {
//                    st.setIsCancelled(true);
//                    st.stopTask();
//                }
//                    mcs.stopRecording();
//                    flag = false;
//            }



//        });



    }


    private void checkPermission() {
        int hasPermission = 0;
        for (String permission: permissions) {
            hasPermission += ContextCompat.checkSelfPermission(getApplication(), permission);
        }
        if (hasPermission == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "权限已获得", Toast.LENGTH_LONG).show();
        }else{
            //没有权限，申请权限。
            ActivityCompat.requestPermissions(this,
                    permissions,
                    1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (!Environment.isExternalStorageManager()){
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                startActivityForResult(intent, 1);
            }
        }
    }






    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            int granted = 0;
            if (grantResults.length > 0){
                for (int grantResult : grantResults) {
                    granted += grantResult;
                }
            }
            if (grantResults.length <= 0 || granted < 0){
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this, "权限申请成功", Toast.LENGTH_LONG).show();
            }
        }
    }

}