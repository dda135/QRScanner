/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package indi.fanjh.qrscanlib.core;

import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.google.zxing.Result;

/**
 * @author fanjh
 * @date 2018/3/30 14:21
 * @description 扫描执行类
 * @note
 **/
final class CaptureActivityHandler extends Handler {
    /**
     * 重新开始识别二维码
     */
    private static final int MSG_RESTART_DECODE = 1;
    /**
     * 图像解析成功
     */
    private static final int MSG_DECODE_SUCCESS = 2;
    /**
     * 图像解析失败
     */
    private static final int MSG_DECODE_FAIL = 3;
    /**
     * 暂停识别任务
     */
    private static final int MSG_PAUSE_DECODE = 4;

    private HandlerThread thread;
    private ScanHandler scanHandler;
    private final CaptureActivityDelegate activity;
    private final CameraManager cameraManager;

    CaptureActivityHandler(CaptureActivityDelegate activity, CameraManager cameraManager, boolean shouldScan) {
        this.activity = activity;
        //启动子线程用于处理解码任务
        thread = new HandlerThread("scan_thread");
        thread.start();
        scanHandler = new ScanHandler(thread.getLooper(), cameraManager, this);

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager;

        cameraManager.startPreview();
        if(shouldScan){
            sendMessageDelayed(Message.obtain(this, CaptureActivityHandler.MSG_RESTART_DECODE),100);
        }
    }

    @Override
    public void handleMessage(Message message) {
        if(null == thread){
            return;
        }
        switch (message.what) {
            case MSG_DECODE_SUCCESS:
                activity.decodeSuccess((Result) message.obj);
                break;
            case MSG_DECODE_FAIL:
            case MSG_RESTART_DECODE:
                cameraManager.requestPreviewFrame(scanHandler);
                break;
            case MSG_PAUSE_DECODE:
                cameraManager.requestPreviewFrame(null);
                break;
            default:
                break;
        }
    }

    /**
     * 终止当前执行者
     */
    void shutDown() {
        cameraManager.stopPreview();

        if(null != scanHandler) {
            scanHandler.removeCallbacksAndMessages(null);
            scanHandler = null;
        }

        removeCallbacksAndMessages(null);

        thread.quit();
        thread = null;
    }

    /**
     * 解析成功
     * @param rawResult 结果
     */
    void decodeSuccess(Result rawResult){
        Message.obtain(this, CaptureActivityHandler.MSG_DECODE_SUCCESS, rawResult).sendToTarget();
    }

    /**
     * 解析失败
     */
    void decodeFail(){
        Message.obtain(this, CaptureActivityHandler.MSG_DECODE_FAIL).sendToTarget();
    }

    /**
     * 重新开始解析
     */
    void restartDecode(){
        Message.obtain(this, CaptureActivityHandler.MSG_RESTART_DECODE).sendToTarget();
    }

    /**
     * 暂停解析
     */
    void pauseDecode(){
        Message.obtain(this, CaptureActivityHandler.MSG_PAUSE_DECODE).sendToTarget();
    }

    Rect getScanRect(){
        return activity.getCropRect();
    }

}
