# 概述
module基于zxing包装了一些基础操作，从而使得扫描二维码操作统一化，并且轻量化，易于移植在多个项目中使用<br>
[zxing地址](https://github.com/zxing/zxing)

# 说明
1.默认实现为横屏扫描，因为相机默认预览成像就是横屏的，我也见过自己手动进行像素数组翻转的，但是相对会消耗一些时间，是不太值得的，因为横屏本来就可以识别<br>
2.默认实现为全屏扫描，经过个人测试，全屏和截取部分扫描，单次差距在20ms左右，没有太大的差距，所以默认为全屏，如果因为业务问题会经常出现多个二维码在一个屏幕内
的情况，内部也有提供指定矩形的方式指定扫描区域（坐标要横向计算）<br>
3.默认提供了ZXingHelper类来解析静态的图片二维码，比方说解析相册里面的二维码图片<br>
4.默认有EncodingUtils生成二维码<br>

# 简单的使用例子
需要在AndroidManifest.xml里面申请的权限
```
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.VIBRATE"/>
```

```
    private CaptureActivityDelegate captureActivityDelegate = new CaptureActivityDelegate(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        captureActivityDelegate.onPreSetContentView();
        setContentView(R.layout.activity_capture_demo);
        surfaceView = findViewById(R.id.capture_preview);
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
        captureActivityDelegate.onResume(true);
    }

    @Override
    protected void onPause() {
        captureActivityDelegate.onPrePause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        captureActivityDelegate.onPreDestroy();
        super.onDestroy();
    }
```

1.扫描和实际的UI交互是互不干涉的，核心的扫描只需要一个SurfaceView，其余UI需要自己额外实现<br>
2.扫描默认要求相机权限，在targetAPI大于等于23的情况下，必须申请权限后使用<br>
