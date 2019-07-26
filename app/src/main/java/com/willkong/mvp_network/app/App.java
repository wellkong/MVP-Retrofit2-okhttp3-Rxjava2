package com.willkong.mvp_network.app;

import android.app.Application;

import com.hjq.toast.ToastUtils;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network
 * @Author: willkong
 * @CreateDate: 2019/7/18 11:06
 * @Description: 工程Application
 */
public class App extends Application {
    public static App mInstance;

    public static App instance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mInstance = this;
        // 在 Application 中初始化
        ToastUtils.init(this);
        initLogger();
    }

    /**
     * 初始化logger框架
     */
    private void initLogger() {
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)  // 是否显示线程信息 默认显示 上图Thread Infrom的位置
                .methodCount(0)         // 展示方法的行数 默认是2  上图Method的行数
                .methodOffset(7)        // 内部方法调用向上偏移的行数 默认是0
//                .logStrategy(customLog) // 改变log打印的策略一种是写本地，一种是logcat显示 默认是后者（当然也可以自己定义）
                .tag("willkong")   // 自定义全局tag 默认：PRETTY_LOGGER
                .build();
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy) {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return true;
            }
        });
    }
}
