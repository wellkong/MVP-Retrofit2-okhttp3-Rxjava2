package com.willkong.mvp_network.base.cookie;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.cookie
 * @Author: willkong
 * @CreateDate: 2019/7/18 11:51
 * @Description: cookie 处理
 */
public class CookieManger implements CookieJar {
    private static PersistentCookieStore cookieStore;

    public CookieManger(Context context) {
        if (cookieStore == null) {
            cookieStore = new PersistentCookieStore(context);
        }
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        if (cookies != null && cookies.size() > 0) {
            for (Cookie item : cookies) {
                cookieStore.add(url, item);
                if (item.name() != null && !TextUtils.isEmpty(item.name()) &&
                        item.value() != null && !TextUtils.isEmpty(item.value())) {
                    /*保存cookie到sp地方  可能会用到 */
//                    PrefUtils.setString(mContext, "cookie_name", item.name());
//                    PrefUtils.setString(mContext, "cookie_value", item.value());
                }
            }
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = cookieStore.get(url);
        for (int i = 0; i < cookies.size(); i++) {
            Log.e("", "拿出来的cookies name()==" + cookies.get(i).name());
            Log.e("", "拿出来的cookies value()==" + cookies.get(i).value());
        }
        return cookies;
    }
}
