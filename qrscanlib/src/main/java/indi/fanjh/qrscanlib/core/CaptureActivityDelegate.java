package indi.fanjh.qrscanlib.core;

import android.app.Activity;
import android.graphics.Rect;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.google.zxing.Result;


import java.io.IOException;

import indi.fanjh.qrscanlib.utils.BeepManager;
import indi.fanjh.qrscanlib.utils.InactivityTimer;

/**
* @author fanjh
* @date 2018/3/30 11:33
* @description 扫码页面代理
* @note 通过该代理进行操作能够很方便的解耦UI和实际扫码操作
**/
public class CaptureActivityDelegate {
    private Activity activity;
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private SurfaceView scanPreview = null;
    private Rect mCropRect = null;
    private Rect mPreviewRect = null;
    private boolean isHasSurface = false;
    private OnStatusListener onStatusListener;
    private boolean shouldScan;
    private int previewWidth;
    private int previewHeight;

    /**
     * 设置一些状态的监听
     * @param onStatusListener
     */
    public void setOnStatusListener(OnStatusListener onStatusListener) {
        this.onStatusListener = onStatusListener;
    }

    private SurfaceHolder.Callback surfaceHolder = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (!isHasSurface) {
                isHasSurface = true;
                initCamera(holder);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            isHasSurface = false;
        }
    };

    public CaptureActivityDelegate(Activity activity) {
        this.activity = activity;
    }

    /**
     * 在onCreate->setContentView之前调用
     */
    public void onPreSetContentView(){
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 在onCreate中setContentView之后调用
     * @param surfaceView 当前相机预览要关联的surfaceview
     */
    public void onPostSetContentView(@NonNull SurfaceView surfaceView){
        scanPreview = surfaceView;
        inactivityTimer = new InactivityTimer(activity);
        beepManager = new BeepManager(activity);
    }

    /**
     * 自定义裁剪区域，一般来说就是优化一下速度
     * 不过实际测试中会发现，效果并不明显，一般来说可以采用全屏的操作
     * @param rect 相对于surfaceView的区域
     * @param previewRect surfaceView的区域
     */
    public void setCropRect(Rect rect,Rect previewRect){
        this.mCropRect = rect;
        this.mPreviewRect = previewRect;
        if(null != mPreviewRect){
            previewHeight = mPreviewRect.height();
            previewWidth = mPreviewRect.width();
        }
    }

    public void onResume(boolean shouldScan){
        this.shouldScan = shouldScan;
        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        cameraManager = new CameraManager(activity);

        handler = null;

        if (isHasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(scanPreview.getHolder());
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            scanPreview.getHolder().addCallback(surfaceHolder);
        }

        inactivityTimer.onResume();
    }

    public void onPrePause(){
        if (handler != null) {
            handler.shutDown();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(surfaceHolder);
        }
    }

    public void onPreDestroy(){
        inactivityTimer.shutdown();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {

            cameraManager.openDriver(surfaceHolder);

            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager,shouldScan);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            e.printStackTrace();
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        if(null != onStatusListener){
            onStatusListener.onCameraOpenError();
        }
    }

    public Rect getCropRect() {
        if(null == mPreviewRect){
            return mCropRect;
        }
        Camera.Size size = cameraManager.getPreviewSize();
        int width = size.width;
        int height = size.height;

        if(previewHeight != height || previewWidth != width) {
            previewWidth = width;
            previewHeight = height;

            double widthRatio = width * 1.0 / mPreviewRect.width();
            double heightRatio = height * 1.0 / mPreviewRect.height();
            mCropRect.left = (int) (mCropRect.left * widthRatio);
            mCropRect.right = (int) (mCropRect.right * widthRatio);
            mCropRect.top = (int) (mCropRect.top * heightRatio);
            mCropRect.bottom = (int) (mCropRect.bottom * heightRatio);
        }
        return mCropRect;
    }

    /**
     * 解析成功
     * @param rawResult 解析结果
     */
    void decodeSuccess(Result rawResult) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();

        if(null != onStatusListener){
            onStatusListener.onScanSuccess(rawResult.getText());
        }
    }

    /**
     * 暂停解析任务
     */
    public void pauseDecode(){
        if(null != handler) {
            handler.pauseDecode();
        }
    }

    /**
     * 重新开始解析任务
     */
    public void restartDecode(){
        if(null != handler) {
            handler.restartDecode();
        }
    }

    public void setTorch(boolean isTorch){
        if(null != cameraManager) {
            cameraManager.setTorch(isTorch);
        }
    }

}
