package com.willkong.mvp_network.base.mvp;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.mvp
 * @Author: willkong
 * @CreateDate: 2019/7/25 17:16
 * @Description: 基本回调 可自定义添加所需回调
 */

public interface BaseView {
    /**
     * 显示dialog
     */
    void showLoading();
    /**
     * 隐藏 dialog
     */

    void hideLoading();
    /**
     * 显示错误信息
     *
     * @param msg
     */
    void showError(String msg);
    /**
     * 后台返回错误码
     */
    void onErrorCode(BaseModel model);
}
