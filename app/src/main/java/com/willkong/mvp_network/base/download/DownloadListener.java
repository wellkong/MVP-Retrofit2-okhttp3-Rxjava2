package com.willkong.mvp_network.base.download;

import java.io.File;

/**
 * Created by willkong on 2019/07/20.
 */

public interface DownloadListener {
    void onFinish(File file);

    void onProgress(int progress);

    void onFailed(String errMsg);
}