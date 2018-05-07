/*
 * Copyright (C) 2012 ZXing authors
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

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author fanjh
 * @date 2018/3/30 16:06
 * @description 处理自动聚焦
 * @note 优先使用FOCUS_MODE_CONTINUOUS_PICTURE，采用照相级别聚焦方案
 * 否则采用普通的聚焦模式
 **/
final class AutoFocusManager implements Camera.AutoFocusCallback {
    private static final int MSG_FOCUS = 1;
    /**
     * 聚焦间隔
     */
    private static final long AUTO_FOCUS_INTERVAL_MS = 1200L;
    private static final int STATUS_UNABLE_FOCUS = 1;
    private static final int STATUS_AUTO_FOCUS = 2;
    private static final int STATUS_CONTINUOUS_PICTURE = 3;
    private int focusMode;
    private final Camera camera;
    private boolean stopped;
    private AtomicBoolean focusing = new AtomicBoolean();
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_FOCUS:
                    startFocus();
                    break;
                default:
                    break;
            }
        }
    };

    AutoFocusManager(Context context, Camera camera) {
        this.camera = camera;
        List<String> supportMode = camera.getParameters().getSupportedFocusModes();
        if (supportMode.size() == 0) {
            focusMode = STATUS_UNABLE_FOCUS;
        } else {
            for (String temp : supportMode) {
                if (Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(temp)) {
                    focusMode = STATUS_CONTINUOUS_PICTURE;
                    camera.getParameters().setFocusMode(temp);
                    break;
                } else if (Camera.Parameters.FOCUS_MODE_AUTO.equals(temp) ||
                        Camera.Parameters.FOCUS_MODE_MACRO.equals(temp)) {
                    focusMode = STATUS_AUTO_FOCUS;
                }
            }
        }
        startFocus();
    }

    @Override
    public void onAutoFocus(boolean success, Camera theCamera) {
        focusing.set(false);
        if(focusMode != STATUS_UNABLE_FOCUS) {
            tryFocus();
        }
    }

    /**
     * 尝试去聚焦
     * 目前内部有延时
     */
    private void tryFocus() {
        if (focusMode != STATUS_UNABLE_FOCUS && !stopped && !focusing.get()) {
            handler.sendEmptyMessageDelayed(MSG_FOCUS, AUTO_FOCUS_INTERVAL_MS);
        }
    }

    private void startFocus() {
        if (focusMode != STATUS_UNABLE_FOCUS) {
            if (!stopped && !focusing.get()) {
                try {
                    camera.autoFocus(this);
                    focusing.set(true);
                } catch (RuntimeException re) {
                    try {
                        if (focusMode == STATUS_CONTINUOUS_PICTURE) {
                            List<String> supportMode = camera.getParameters().getSupportedFocusModes();
                            for (String temp : supportMode) {
                                if (Camera.Parameters.FOCUS_MODE_AUTO.equals(temp) ||
                                        Camera.Parameters.FOCUS_MODE_MACRO.equals(temp)) {
                                    focusMode = STATUS_AUTO_FOCUS;
                                    break;
                                }
                            }
                        }
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                    // Try again later to keep cycle going
                    tryFocus();
                }
            }
        }
    }

    void stopFocus() {
        stopped = true;
        if (null != handler) {
            handler.removeCallbacksAndMessages(null);
        }
        if (focusMode != STATUS_UNABLE_FOCUS) {
            try {
                camera.cancelAutoFocus();
            } catch (RuntimeException re) {
                // Have heard RuntimeException reported in Android 4.0.x+;
                // continue?
            }
        }
    }

}
