package com.willkong.mvp_network.core.main;

import android.Manifest;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.willkong.mvp_network.R;
import com.willkong.mvp_network.base.BaseActivity;
import com.willkong.mvp_network.base.download.DownloadListener;
import com.willkong.mvp_network.base.download.DownloadUtil;
import com.willkong.mvp_network.base.mvp.BaseModel;
import com.willkong.mvp_network.core.bean.MainBean;
import com.willkong.mvp_network.utils.RetrofitUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity<MainPresenter> implements MainView, View.OnClickListener {
    private TextView mTvText;
    private TextView tvProgress;
    private TextView tvFileLocation;

    private List<MainBean> mainBeans;

    @Override
    protected MainPresenter createPresenter() {
        return new MainPresenter(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        findViewById(R.id.btn_first).setOnClickListener(this);
        findViewById(R.id.btn_second).setOnClickListener(this);
        findViewById(R.id.btn_third).setOnClickListener(this);
        mTvText = findViewById(R.id.tv_text);
        tvProgress = findViewById(R.id.tv_progess);
        tvFileLocation = findViewById(R.id.tv_file_location);
        mainBeans = new ArrayList<>();
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public void onMainSuccess(BaseModel<List<MainBean>> o) {
        Log.e(o.getErrmsg(), "");
        Log.e(o.getErrcode() + "", "");
        mainBeans.addAll(o.getData());
        Log.e("MainActivity", mainBeans.toString() + "");
        mTvText.setText(o.getData().toString());
    }

    @Override
    public void onUpLoadImgSuccess(BaseModel<Object> o) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_first:
                mPresenter.getMainApi3();
                break;
            case R.id.btn_second:
                downLoadFile();
//                /**
//                 * 俩个参数  一个是图片集合路径   一个是和后台约定的Key，如果后台不需要，随便写都行
//                 */
//                List<String> strings = new ArrayList<>();
//                for (int i = 0; i < 10; i++) {
//                    strings.add("tupian.lujing");
//                }
//                mPresenter.upLoadImgApi(
//                        "title",
//                        "content",
//                        RetrofitUtil.filesToMultipartBodyParts(RetrofitUtil.initImages(strings), "tupian.key"));
                break;
        }
    }

    /**
     * 下载文件
     */
    private void downLoadFile(){
        final String desFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sstx.apk";
        final String baseUrl = "http://www.apk.anzhi.com/";
        final String url = "data4/apk/201809/06/f2a4dbd1b6cc2dca6567f42ae7a91f11_45629100.apk";
        DownloadUtil.getInstance()
                .downloadFile(baseUrl, url, desFilePath, new DownloadListener() {
                    @Override
                    public void onFinish(final File file) {
                        tvFileLocation.setText("下载的文件地址为：" + file.getAbsolutePath());
                        showToast("下载完成");
                    }

                    @Override
                    public void onProgress(int progress) {
                        tvProgress.setText(String.format("下载进度为：%s", progress));
                    }

                    @Override
                    public void onFailed(String errMsg) {
                        showToast("下载失败"+errMsg);
                    }
                });
    }
}
