package indi.fanjh.qrcodescanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import indi.fanjh.qrscanlib.core.CaptureActivityDelegate;
import indi.fanjh.qrscanlib.core.OnStatusListener;

public class MainActivity extends AppCompatActivity {
    private static final int CODE_PERMISSION_CAMERA = 1;
    private SurfaceView surfaceView;
    private ImageView scanLine;
    private Button btnTorch;
    private CaptureActivityDelegate captureActivityDelegate = new CaptureActivityDelegate(this);
    private boolean isTorch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        captureActivityDelegate.onPreSetContentView();
        setContentView(R.layout.activity_capture_demo);
        surfaceView = findViewById(R.id.capture_preview);
        btnTorch = findViewById(R.id.btn_torch);
        scanLine = findViewById(R.id.capture_scan_line);
        btnTorch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTorch = !isTorch;
                captureActivityDelegate.setTorch(isTorch);
            }
        });
        captureActivityDelegate.onPostSetContentView(surfaceView);
        captureActivityDelegate.setOnStatusListener(new OnStatusListener() {
            @Override
            public void onCameraOpenError() {
                Toast.makeText(getApplicationContext(), "当前相机启动失败，可能是没有权限导致！", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onScanSuccess(String result) {
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                //自动会停止扫描
                //这里手动开启
                captureActivityDelegate.restartDecode();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean shouldScan = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        captureActivityDelegate.onResume(shouldScan);
        if (!shouldScan) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CODE_PERMISSION_CAMERA);
        }
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation
                .RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.98f);
        animation.setDuration(1800);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);
    }

    @Override
    protected void onPause() {
        captureActivityDelegate.onPrePause();
        super.onPause();
        scanLine.clearAnimation();
    }

    @Override
    protected void onDestroy() {
        captureActivityDelegate.onPreDestroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CODE_PERMISSION_CAMERA:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    captureActivityDelegate.restartDecode();
                }
                break;
            default:
                break;
        }
    }
}
