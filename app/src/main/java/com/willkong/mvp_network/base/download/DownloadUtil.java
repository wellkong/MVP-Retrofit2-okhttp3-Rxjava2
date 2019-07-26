package com.willkong.mvp_network.base.download;


import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;


public class DownloadUtil {
    private static final String TAG = DownloadUtil.class.getSimpleName();
    private static final int DEFAULT_TIMEOUT = 15;

    private OkHttpClient.Builder mBuilder;
    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    private DownloadUtil() {
    }

    private static class SingletonHolder {
        private static final DownloadUtil INSTANCE = new DownloadUtil();
    }

    public static DownloadUtil getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void initConfig(OkHttpClient.Builder builder) {
        this.mBuilder = builder;
    }

    /**
     * download file and show the progress
     *
     * @param baseUrl
     * @param rUrl     related url
     * @param filePath the path of downloaded file
     * @param listener
     */
    public void downloadFile(final String baseUrl, final String rUrl, final String filePath, final DownloadListener listener) {
        final Executor executor = new MainThreadExecutor();
        DownloadInterceptor interceptor = new DownloadInterceptor(executor, listener);
        if (mBuilder != null) {
            mBuilder.addInterceptor(interceptor);
        } else {
            mBuilder = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .retryOnConnectionFailure(true)
                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        }
        final DownloadService api = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(mBuilder.build())
                .build()
                .create(DownloadService.class);
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Response<ResponseBody> result = api.downloadWithDynamicUrl(rUrl).execute();
                    final File file = FileUtil.writeFile(filePath, result.body().byteStream());
                    if (listener != null) {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                listener.onFinish(file);
                            }
                        });
                    }

                } catch (final IOException e) {
                    if (listener != null) {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                listener.onFailed(e.getMessage());
                            }
                        });
                    }
                    e.printStackTrace();
                }
            }
        });
    }

    private class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable r) {
            handler.post(r);
        }
    }


    /**
     * 使用示例下载文件
     * 这里需要注意的是，下载文件需要申请权限，否则下载直接跳到下载完成回调
     * 在activity中申请权限
     *  ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
     */
    private void downLoadFile(){
        final String desFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sstx.apk";
        final String baseUrl = "http://www.apk.anzhi.com/";
        final String url = "data4/apk/201809/06/f2a4dbd1b6cc2dca6567f42ae7a91f11_45629100.apk";
        DownloadUtil.getInstance()
                .downloadFile(baseUrl, url, desFilePath, new DownloadListener() {
                    @Override
                    public void onFinish(final File file) {
                        //下载完成
                    }

                    @Override
                    public void onProgress(int progress) {
//                        tvProgress.setText(String.format("下载进度为：%s", progress));
                    }

                    @Override
                    public void onFailed(String errMsg) {
                        //下载失败
                    }
                });
    }
}
