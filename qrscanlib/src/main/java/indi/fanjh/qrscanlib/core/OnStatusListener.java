package indi.fanjh.qrscanlib.core;

/**
* @author fanjh
* @date 2018/4/23 13:59
* @description 一些状态的监听
* @note
**/
public interface OnStatusListener {
    /**
     * 相机启动失败
     * 目前明确发现的问题是一些低于Android6.0的机型存在厂商自定义权限管理的问题，导致缺少相机权限
     * 所以一般需要在这里进行权限检查
     */
    void onCameraOpenError();

    /**
     * 扫描成功
     *
     * @param result
     */
    void onScanSuccess(String result);
}