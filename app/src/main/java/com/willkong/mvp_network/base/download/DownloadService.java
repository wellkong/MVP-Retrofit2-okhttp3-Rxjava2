package com.willkong.mvp_network.base.download;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by willkong on 2019/07/20.
 */

public interface DownloadService {
    @Streaming
    @GET
    Call<ResponseBody> downloadWithDynamicUrl(@Url String fileUrl);
}
