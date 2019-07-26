package com.willkong.mvp_network.base.mvp;

import java.io.Serializable;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.model
 * @Author: willkong
 * @CreateDate: 2019/7/25 16:50
 * @Description: mode基类
 */
public class BaseModel<T> implements Serializable {
    private String msg;
    private int code;
    private T data;

    public BaseModel(String message, int code) {
        this.msg = message;
        this.code = code;
    }

    public int getErrcode() {
        return code;
    }

    public void setErrcode(int code) {
        this.code = code;
    }

    public String getErrmsg() {
        return msg;
    }

    public void setErrmsg(String message) {
        this.msg = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T result) {
        this.data = result;
    }

    @Override
    public String toString() {
        return "BaseModel{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", result=" + data +
                '}';
    }
}
