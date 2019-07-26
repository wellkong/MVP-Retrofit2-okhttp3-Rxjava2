package com.willkong.mvp_network.base.mvp;

import com.willkong.mvp_network.api.ApiRetrofit;
import com.willkong.mvp_network.api.ApiServer;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.mvp
 * @Author: willkong
 * @CreateDate: 2019/7/25 17:17
 * @Description: MVP presenter基类
 */
public class BasePresenter<V extends BaseView> {
    private CompositeDisposable compositeDisposable;
    public V baseView;
    protected ApiServer apiServer = ApiRetrofit.getInstance().getApiService();
    public BasePresenter(V baseView) {
        this.baseView = baseView;
    }

    /**
     * 返回 view
     *
     * @return
     */
    public V getBaseView() {
        return baseView;
    }
    /**
     * 解除绑定
     */
    public void detachView() {
        baseView = null;
        removeDisposable();
    }

    public void addDisposable(Observable<?> observable, BaseObserver observer) {
        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
        }
        compositeDisposable.add(observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(observer));
    }

    public void removeDisposable() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }
}
