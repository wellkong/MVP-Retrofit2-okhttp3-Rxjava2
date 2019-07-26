package com.willkong.mvp_network.core.main;

import com.willkong.mvp_network.base.mvp.BaseModel;
import com.willkong.mvp_network.base.mvp.BaseView;
import com.willkong.mvp_network.core.bean.MainBean;

import java.util.List;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.core.main
 * @Author: willkong
 * @CreateDate: 2019/7/26 9:32
 * @Description: MainActivity的接口数据回调类
 */
public interface MainView extends BaseView{
    /**
     * 数据请求成功
     * @param o
     */
    void onMainSuccess(BaseModel<List<MainBean>> o);

    /**
     * 图片上传成功
     * @param o
     */
    void onUpLoadImgSuccess(BaseModel<Object> o);
}
