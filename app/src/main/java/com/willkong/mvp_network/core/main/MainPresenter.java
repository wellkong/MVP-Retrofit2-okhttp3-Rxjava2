package com.willkong.mvp_network.core.main;

import com.willkong.mvp_network.base.mvp.BaseModel;
import com.willkong.mvp_network.base.mvp.BaseObserver;
import com.willkong.mvp_network.base.mvp.BasePresenter;
import com.willkong.mvp_network.core.bean.MainBean;
import com.willkong.mvp_network.utils.RetrofitUtil;

import java.util.HashMap;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.core.main
 * @Author: willkong
 * @CreateDate: 2019/7/26 9:35
 * @Description: MainActivity对应的请求Presenter
 */
public class MainPresenter extends BasePresenter<MainView> {
    public MainPresenter(MainView baseView) {
        super(baseView);
    }

    /**
     * 请求获取主页数据
     */
    public void getMainApi() {
        addDisposable(apiServer.getMain("year"), new BaseObserver(baseView) {
            @Override
            public void onSuccess(BaseModel o) {
                baseView.onMainSuccess(o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }

    /**
     * 写法好多种  怎么顺手怎么来
     */
    public void getMainApi2() {
        addDisposable(apiServer.getMain2("year"), new BaseObserver(baseView) {
            @Override
            public void onSuccess(BaseModel o) {
                baseView.onMainSuccess(o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }

    /**
     * 写法好多种  怎么顺手怎么来
     */
    public void getMainApi3() {
        HashMap<String, String> params = new HashMap<>();
        params.put("time", "year");
        addDisposable(apiServer.getMain3(params), new BaseObserver(baseView) {

            @Override
            public void onSuccess(BaseModel o) {
                baseView.onMainSuccess(o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }


    /*>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  图片上传  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/

    /**
     * 演示单图上传
     *
     * @param parts
     */
    public void upLoadImgApi(MultipartBody.Part parts) {
        addDisposable(apiServer.upLoadImg(parts), new BaseObserver(baseView) {

            @Override
            public void onSuccess(BaseModel o) {
                baseView.onUpLoadImgSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }


    /**
     * 演示多图上传
     *
     * @param parts
     */
    public void upLoadImgApi(List<MultipartBody.Part> parts) {
        addDisposable(apiServer.upHeadImg(parts), new BaseObserver(baseView) {
            @Override
            public void onSuccess(BaseModel o) {
                baseView.onUpLoadImgSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }

    /**
     * 演示 图片和字段一起上传
     *
     * @param title
     * @param content
     * @param parts
     */
    public void upLoadImgApi(String title, String content, List<MultipartBody.Part> parts) {
        HashMap<String, RequestBody> params = new HashMap<>();
        params.put("title", RetrofitUtil.convertToRequestBody(title));
        params.put("content", RetrofitUtil.convertToRequestBody(content));
        addDisposable(apiServer.expressAdd(params, parts), new BaseObserver(baseView) {
            @Override
            public void onSuccess(BaseModel o) {
                baseView.onUpLoadImgSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }


}
