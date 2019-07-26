package com.willkong.mvp_network.base.download;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

/**
 * Created by willkong on 2019/07/20.
 *
 * @modifier
 * @description
 */
public final class FileUtil {
    /**
     * SD卡根目录
     */
    public static final String SD_HOME_DIR = Environment.getExternalStorageDirectory().getPath() + "/willkong/";
    private FileUtil() {
    }

    public static File writeFile(String filePath, InputStream input) {
        if (input == null)
            return null;
        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file);
             InputStream ins = input) {
            byte[] b = new byte[1024];
            int len;
            while ((len = ins.read(b)) != -1) {
                fos.write(b, 0, len);
            }
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }


    /**
     * 下载到本地
     *
     * @param body 内容
     * @return 成功或者失败
     */
    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            //判断文件夹是否存在
            File files = new File(SD_HOME_DIR);
            if (!files.exists()) {
                //不存在就创建出来
                files.mkdirs();
            }
            //创建一个文件
            File futureStudioIconFile = new File(SD_HOME_DIR + "download.jpg");
            //初始化输入流
            InputStream inputStream = null;
            //初始化输出流
            OutputStream outputStream = null;

            try {
                //设置每次读写的字节
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;
                //请求返回的字节流
                inputStream = body.byteStream();
                //创建输出流
                outputStream = new FileOutputStream(futureStudioIconFile);
                //进行读取操作
                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }
                    //进行写入操作
                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;

                }
                //刷新
                outputStream.flush();
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    //关闭输入流
                    inputStream.close();
                }

                if (outputStream != null) {
                    //关闭输出流
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}
