package indi.fanjh.qrscanlib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author fanjh
 * @date 2018/3/29 16:55
 * @description 基于Zxing库的扫描二维码帮助类
 * @note
 **/
public class ZxingHelper {
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("ZxingHelper");
            return thread;
        }
    });
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private final MultiFormatReader multiFormatReader;
    private OnResultCallback onResultCallback;

    /**
     * 设置结果回调监听
     *
     * @param onResultCallback 结果回调
     */
    public void setOnResultCallback(OnResultCallback onResultCallback) {
        this.onResultCallback = onResultCallback;
    }

    /**
     * 扫描结果回调
     */
    public interface OnResultCallback {
        /**
         * 扫描成功
         *
         * @param result 扫描后获得的文本结果
         */
        void onSuccess(String result);

        /**
         * 扫描失败
         */
        void onFail();
    }

    public ZxingHelper() {
        multiFormatReader = new MultiFormatReader();
        //使用默认支持，实际上包括二维码、MaxiCode等
        //虽然好像用不到
        multiFormatReader.setHints(null);
    }

    /**
     * 基于相机进行解码
     *
     * @param data   当前需要解码的图像像素数据
     * @param width  当前图像的宽度
     * @param height 当前图像的高度
     * @param rect   当前的扫码区域
     * @return 目前只支持文本，因为方便转义成具体的业务操作
     */
    public void decodeForCamera(byte[] data, int width, int height, Rect rect) {
        Result rawResult = null;
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                rect.width(), rect.height(), false);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            rawResult = multiFormatReader.decodeWithState(bitmap);
            if (null != rawResult) {
                if (null != onResultCallback) {
                    onResultCallback.onSuccess(rawResult.getText());
                }
            }
        } catch (ReaderException re) {
            // continue
            if (null != onResultCallback) {
                onResultCallback.onFail();
            }
        } finally {
            multiFormatReader.reset();
        }
    }

    /**
     * 识别静态图片
     *
     * @param path 图片路径
     */
    public void decodeForPicture(final String path) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                Bitmap scanBitmap = BitmapFactory.decodeFile(path, options);
                options.inJustDecodeBounds = false;
                int sampleSize = Math.max((int) (options.outHeight / (float) 200), (int) (options.outWidth / (float) 200));
                if (sampleSize <= 0) {
                    sampleSize = 1;
                }
                options.inSampleSize = sampleSize;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                scanBitmap = BitmapFactory.decodeFile(path, options);
                final int width = scanBitmap.getWidth();
                final int height = scanBitmap.getHeight();
                final int[] pixels = new int[width * height];
                scanBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                decodeForPicture(pixels, width, height);
            }
        });
    }

    /**
     * 基于图片进行解码
     *
     * @param data   当前需要解码的图片像素数据
     * @param width  当前图像的宽度
     * @param height 当前图像的高度
     * @return 目前只支持文本，因为方便转义成具体的业务操作
     */
    private void decodeForPicture(int[] data, int width, int height) {
        Result rawResult = null;
        LuminanceSource source = new RGBLuminanceSource(width, height, data);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            rawResult = multiFormatReader.decode(bitmap);
            if (null != rawResult) {
                if (null != onResultCallback) {
                    final Result finalRawResult = rawResult;
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            onResultCallback.onSuccess(finalRawResult.getText());
                        }
                    });
                }
            }
        } catch (ReaderException re) {
            // continue
            if (null != onResultCallback) {
                HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        onResultCallback.onFail();
                    }
                });
            }
        } finally {
            multiFormatReader.reset();
        }
    }

    /**
     * 识别静态图片
     *
     * @param scanBitmap 图片数据
     */
    public void decodeForBitmap(final Bitmap scanBitmap) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                final int width = scanBitmap.getWidth();
                final int height = scanBitmap.getHeight();
                final int[] pixels = new int[width * height];
                scanBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                decodeForPicture(pixels, width, height);
            }
        });
    }

}
