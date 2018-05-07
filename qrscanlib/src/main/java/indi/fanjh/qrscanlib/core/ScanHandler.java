/*
 * Copyright (C) 2010 ZXing authors
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
import android.os.Looper;
import android.os.Message;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

/**
 * @author fanjh
 * @date 2018/3/30 15:23
 * @description 解析处理者
 * @note 应该运行在子线程中
 *
 * update by fanjh on 2018/4/8
 * 优化zxing的扫描速度
 * 因为相机的预览图像默认为横向的，所以说之前的实现为了同视觉一致采用了像素反转的操作
 * 但是这样带来了大量的循环，从而导致解析变慢，从图像解析的角度来说，只要二维码在图中即可
 * 结论：
 * 1.全屏扫描区域的时候没有任何必要进行反转，纯属多余操作，可以提速200ms以上
 * 2.如果需要裁剪，那么裁剪的时候需要注意数据实际上是横向的，其次就是效果相对于全屏来说并不明显
 *
 **/
final class ScanHandler extends Handler {
    private static final int MSG_DECODE = 1;
    private final QRCodeReader multiFormatReader;
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;

    ScanHandler(Looper looper, CameraManager cameraManager, CaptureActivityHandler handler) {
        super(looper);
        this.cameraManager = cameraManager;
        this.handler = handler;

        multiFormatReader = new QRCodeReader();

    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case MSG_DECODE:
                realDecode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            default:
                break;
        }
    }

    private void realDecode(byte[] data, int width, int height) {
        /*Size size = cameraManager.getPreviewSize();

        // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++) {
                rotatedData[x * size.height + size.height - y - 1] = data[x + y * size.width];
            }
        }

        // 宽高也要调整
        int tmp = size.width;
        size.width = size.height;
        size.height = tmp;*/

        Result rawResult = null;
        PlanarYUVLuminanceSource source = buildLuminanceSource(data, width, height);
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decode(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }

        if (rawResult != null) {
            // Don't log the barcode contents for security.
            if (handler != null) {
                handler.decodeSuccess(rawResult);
            }
        } else {
            if (handler != null) {
                handler.decodeFail();
            }
        }
    }

    void decode(byte[] data, int width, int height) {
        Message message = obtainMessage();
        message.what = MSG_DECODE;
        message.obj = data;
        message.arg1 = width;
        message.arg2 = height;
        sendMessage(message);
    }

    private PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = handler.getScanRect();
        if (rect == null) {
            return new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
        }
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
    }

}
