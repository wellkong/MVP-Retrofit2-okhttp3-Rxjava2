package com.willkong.mvp_network.base.mvp;

import com.google.gson.JsonParseException;
import com.willkong.mvp_network.base.ApiException;
import com.willkong.mvp_network.base.BaseContent;
import com.willkong.mvp_network.utils.NetWorkUtils;

import org.json.JSONException;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.ParseException;

import io.reactivex.observers.DisposableObserver;
import retrofit2.HttpException;
/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.mvp
 * @Author: willkong
 * @CreateDate: 2019/7/25 17:17
 * @Description: 数据处理基类
 */

public abstract class BaseObserver<T> extends DisposableObserver<BaseModel<T>> {
    protected BaseView view;
    /**
     * 网络连接失败  无网
     */
    public static final int NETWORK_ERROR = 100000;
    /**
     * 解析数据失败
     */
    public static final int PARSE_ERROR = 1008;
    /**
     * 网络问题
     */
    public static final int BAD_NETWORK = 1007;
    /**
     * 连接错误
     */
    public static final int CONNECT_ERROR = 1006;
    /**
     * 连接超时
     */
    public static final int CONNECT_TIMEOUT = 1005;

    /**
     * 其他所有情况
     */
    public static final int NOT_TRUE_OVER = 1004;


    public BaseObserver(BaseView view) {
        this.view = view;
    }

    @Override
    protected void onStart() {
        if (view != null) {
            view.showLoading();
        }
    }

    @Override
    public void onNext(BaseModel<T> o) {
        T t = o.getData();
        try {
            if (view != null) {
                view.hideLoading();
            }
            //如果添加了goson的自定义解析，MyGsonResponseBodyConverter已经把请求不等于成功码的抛出了异常走onError接口了，可以直接走onSuccess方法回调
            if (o.getErrcode() == BaseContent.basecode) {
                onSuccess(o);
            } else {
                //把错误码回调给页面
                view.onErrorCode(o);
                //非  true的所有情况
                onException(o.getErrcode(), o.getErrmsg());
            }
        } catch (Exception e) {
            e.printStackTrace();
            onError(e.toString());
        }
    }

    @Override
    public void onError(Throwable e) {
        if (view != null) {
            view.hideLoading();
        }
        if (e instanceof HttpException) {
            //   HTTP错误
            onException(BAD_NETWORK, "");
        } else if (e instanceof ConnectException
                || e instanceof UnknownHostException) {
            //   连接错误
            onException(CONNECT_ERROR, "");
        } else if (e instanceof InterruptedIOException) {
            //  连接超时
            onException(CONNECT_TIMEOUT, "");
        } else if (e instanceof JsonParseException
                || e instanceof JSONException
                || e instanceof ParseException) {
            //  解析错误
            onException(PARSE_ERROR, "");
            e.printStackTrace();
            /**
             * 此处很重要
             * 为何这样写：因为开发中有这样的需求   当服务器返回假如0是正常 1是不正常  当返回0时：我们gson 或 fastJson解析数据
             * 返回1时：我们不想解析（可能返回值出现以前是对象 但是现在数据为空变成了数组等等，于是在不改后台代码的情况下  我们前端需要处理）
             * 但是用了插件之后没有很有效的方法控制解析 所以处理方式为  当服务器返回不等于0时候  其他状态都抛出异常 然后提示
             * 代码上一级在 MyGsonResponseBodyConverter 中处理  前往查看逻辑
             */
        } else if (e instanceof ApiException) {
            ApiException exception = (ApiException) e;
            int code = exception.getErrorCode();
            view.onErrorCode(new BaseModel(exception.getMessage(), code));
        }  else {
            if (e != null) {
                onError(e.toString());
            } else {
                onError("未知错误");
            }
        }
    }

    /**
     * 中间拦截一步  判断是否有网络  为确保准确  此步去除也可以
     *
     * @param unknownError
     * @param message
     */
    private void onException(int unknownError, String message) {
        BaseModel model = new BaseModel(message, unknownError);
        if (!NetWorkUtils.isAvailableByPing()) {
            model.setErrcode(NETWORK_ERROR);
            model.setErrmsg("网络不可用，请检查网络连接！");
        }
        onExceptions(model.getErrcode(), model.getErrmsg());
        if (view != null) {
            view.onErrorCode(model);
        }
    }
    private void onExceptions(int unknownError, String message) {
        switch (unknownError) {
            case CONNECT_ERROR:
                onError("连接错误");
                break;
            case CONNECT_TIMEOUT:
                onError("连接超时");
                break;
            case BAD_NETWORK:
                onError("网络超时");
                break;
            case PARSE_ERROR:
                onError("数据解析失败");
                break;
            //非true的所有情况
            case NOT_TRUE_OVER:
                onError(message);
                break;
            default:
                break;
        }
    }

    //消失写到这 有一定的延迟  对dialog显示有影响
    @Override
    public void onComplete() {
       /* if (view != null) {
            view.hideLoading();
        }*/
    }

    public abstract void onSuccess(BaseModel<T> o);

    public abstract void onError(String msg);
}
