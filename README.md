# MVP-Retrofit2-okhttp3-Rxjava2
MVP-Retrofit2-okhttp3-Rxjava2网络请求,开发实用,简约框架
## 前言
目前较火的网络请求有MVP+Retrofit2+okhttp3+Rxjava2，使用这个框架也有一段时间了，也看了一些大神的封装，这里就对别人封装的框架进行总结和收集。

# 任务
相关业务需求及解决方案
- 1、Retrofit配置及各情况处理
- 2、Retrofit，Gson解析，自定义解析内容（如code=1全部解析，code=0不做解析）
- 3、Retrofit，Gson解析，请求返回的类型不统一，假如double返回的是null 
- 4、Retrofit实现cookie自动化管理 
- 5、Retrofit文件上传
- 6、Retrofit文件下载

### 框架搭建步骤
#### 1.导包
在工程的build.gradle的dependencies下导入以下所需的网络依赖包

```
 //网络请求
    implementation 'com.squareup.okhttp3:okhttp:3.11.0'
    implementation 'com.squareup.retrofit2:retrofit:2.4.0'
    //ConverterFactory的Gson依赖包
    implementation 'com.squareup.retrofit2:converter-gson:2.4.0'
    //CallAdapterFactory的Rx依赖包
    implementation 'com.squareup.retrofit2:adapter-rxjava2:2.4.0'
    
    implementation 'io.reactivex.rxjava2:rxandroid:2.1.0'
```
#### 2、retrofit基类代码实现，对日志参数进行了拦截
注：get请求参数打印会拼接在url之后，post打印单独显示
打印框架 [logger](https://github.com/orhanobut/logger),日志结构很直观（推荐使用）

```
 //日志打印框架
    implementation 'com.orhanobut:logger:2.2.0'
```
新建一个全局类App继承Application
App.class初始化使用的logger框架，并在清单文件定义我们的app

清单文件
```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.willkong.mvp_network">

    <application
        android:name=".App"//定义我们的app
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

App.class类
```

import android.app.Application;

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

```
#### 3.Retrofit配置及各情况处理
封装Retrofit
##### 实现目标
- 1、Retrofit创建
- 2、Retrofit实现Cookie自动化管理
- 3、Retrofit，Gson解析，请求返回的类型不统一，假如double返回的是null
- 4、请求参数日志打印
- 5、统一请求参数添加到请求头中
- 6、统一请求参数添加到请求body中
- 7、缓存的拦截器
- 8、BaseUrl动态切换
- 9、拦截指定接口，动态更改返回值便于测试
###### 1、Retrofit创建
在工程目录新建一个文件命名为api，在api文件夹下新建一个类命名为ApiRetrofit
- 1.编写ApiRetrofit对象的单例模式，工程只有一个网络配置实例，防止请求数据混乱。

```
   /**
     * 网络请求类需要设置为单例模式
     * @return
     */
    public static ApiRetrofit getInstance() {
        if (apiRetrofit == null) {
            synchronized (Object.class) {
                if (apiRetrofit == null) {
                    apiRetrofit = new ApiRetrofit();
                }
            }
        }
        return apiRetrofit;
    }
```
###### 2、Retrofit实现Cookie自动化管理
在开发中，我们可能会遇到这样的需求，要长期保持登陆状态，但我们不想接口添加任何参数处理时，这样我们可能会想到cookie
最终实现效果为：登录成功后将服务器返回的cookie保存到本地(每请求成功接口重新更新下本地cookie值)，之后每个接口请求时都将cookie带上，下面介绍俩种实现方法
- 1、已有现成的第三方框架实现
- 2、手写cookie管理类，扩展性强
###### 第一种实现方方法（第三方库实现）
（1）依赖第三方库

```
//cookie管理
    implementation 'com.github.franmontiel:PersistentCookieJar:v1.0.1'
```
（2）创建OkHttpClient时添加cookieJar

```
OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder
                .cookieJar(new CookieManger(App.getContext()))
                .addInterceptor(interceptor)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);//错误重联


        /**
         * 处理一些识别识别不了 ipv6手机，如小米  实现方案  将ipv6与ipv4置换位置，首先用ipv4解析
         */
        httpClientBuilder.dns(new ApiDns());

        /**
         * 添加cookie管理
         * 方法1：第三方框架
         */
        PersistentCookieJar cookieJar = new PersistentCookieJar(new SetCookieCache(),
                new SharedPrefsCookiePersistor(App.getContext()));
        httpClientBuilder.cookieJar(cookieJar);

        /**
         * 添加cookie管理
         * 方法2：手动封装cookie管理
         */
//        httpClientBuilder.cookieJar(new CookieManger(App.getContext()));
```
###### 第二种实现方方法（手写cookie管理类）
- （1）创建CookieManger类实现okhttp3的cookieJar接口

```

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
public class CookieManger implements CookieJar{
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
```
- （2）创建OkHttpCookies类

```

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import okhttp3.Cookie;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.cookie
 * @Author: willkong
 * @CreateDate: 2019/7/18 11:59
 * @Description: OkHttpCookies 缓存的实体类 读取和写入
 */
public class OkHttpCookies implements Serializable{
    private transient final Cookie cookies;
    private transient Cookie clientCookies;

    public OkHttpCookies(Cookie cookies) {
        this.cookies = cookies;
    }
    public Cookie getCookies() {
        Cookie bestCookies = cookies;
        if (clientCookies != null) {
            bestCookies = clientCookies;
        }
        return bestCookies;
    }
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(cookies.name());
        out.writeObject(cookies.value());
        out.writeLong(cookies.expiresAt());
        out.writeObject(cookies.domain());
        out.writeObject(cookies.path());
        out.writeBoolean(cookies.secure());
        out.writeBoolean(cookies.httpOnly());
        out.writeBoolean(cookies.hostOnly());
        out.writeBoolean(cookies.persistent());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String name = (String) in.readObject();
        String value = (String) in.readObject();
        long expiresAt = in.readLong();
        String domain = (String) in.readObject();
        String path = (String) in.readObject();
        boolean secure = in.readBoolean();
        boolean httpOnly = in.readBoolean();
        boolean hostOnly = in.readBoolean();
        boolean persistent = in.readBoolean();
        Cookie.Builder builder = new Cookie.Builder();
        builder = builder.name(name);
        builder = builder.value(value);
        builder = builder.expiresAt(expiresAt);
        builder = hostOnly ? builder.hostOnlyDomain(domain) : builder.domain(domain);
        builder = builder.path(path);
        builder = secure ? builder.secure() : builder;
        builder = httpOnly ? builder.httpOnly() : builder;
        clientCookies =builder.build();
    }
}

```
- （3）创建PersistentCookieStore类

```

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.cookie
 * @Author: willkong
 * @CreateDate: 2019/7/18 12:05
 * @Description: cookie内容操作转换
 */
public class PersistentCookieStore {
    private static final String LOG_TAG = "PersistentCookieStore";
    private static final String COOKIE_PREFS = "Cookies_Prefs";

    private final Map<String, ConcurrentHashMap<String, Cookie>> cookies;
    private final SharedPreferences cookiePrefs;


    public PersistentCookieStore(Context context) {
        cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, 0);
        cookies = new HashMap<>();

        //将持久化的cookies缓存到内存中 即map cookies
        Map<String, ?> prefsMap = cookiePrefs.getAll();
        for (Map.Entry<String, ?> entry : prefsMap.entrySet()) {
            String[] cookieNames = TextUtils.split((String) entry.getValue(), ",");
            for (String name : cookieNames) {
                String encodedCookie = cookiePrefs.getString(name, null);
                if (encodedCookie != null) {
                    Cookie decodedCookie = decodeCookie(encodedCookie);
                    if (decodedCookie != null) {
                        if (!cookies.containsKey(entry.getKey())) {
                            cookies.put(entry.getKey(), new ConcurrentHashMap<String, Cookie>());
                        }
                        cookies.get(entry.getKey()).put(name, decodedCookie);
                    }
                }
            }
        }
    }

    protected String getCookieToken(Cookie cookie) {
        return cookie.name() + "@" + cookie.domain();
    }

    public void add(HttpUrl url, Cookie cookie) {
        String name = getCookieToken(cookie);

        //将cookies缓存到内存中 如果缓存过期 就重置此cookie
        if (!cookie.persistent()) {
            if (!cookies.containsKey(url.host())) {
                cookies.put(url.host(), new ConcurrentHashMap<String, Cookie>());
            }
            cookies.get(url.host()).put(name, cookie);
        } else {
            if (cookies.containsKey(url.host())) {
                cookies.get(url.host()).remove(name);
            }
        }

        //讲cookies持久化到本地
        SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
        prefsWriter.putString(url.host(), TextUtils.join(",", cookies.get(url.host()).keySet()));
        prefsWriter.putString(name, encodeCookie(new OkHttpCookies(cookie)));
        prefsWriter.apply();
    }

    public List<Cookie> get(HttpUrl url) {
        ArrayList<Cookie> ret = new ArrayList<>();
        if (cookies.containsKey(url.host())) {
            ret.addAll(cookies.get(url.host()).values());
        }
        return ret;
    }

    public boolean removeAll() {
        SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
        prefsWriter.clear();
        prefsWriter.apply();
        cookies.clear();
        return true;
    }

    public boolean remove(HttpUrl url, Cookie cookie) {
        String name = getCookieToken(cookie);

        if (cookies.containsKey(url.host()) && cookies.get(url.host()).containsKey(name)) {
            cookies.get(url.host()).remove(name);

            SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
            if (cookiePrefs.contains(name)) {
                prefsWriter.remove(name);
            }
            prefsWriter.putString(url.host(), TextUtils.join(",", cookies.get(url.host()).keySet()));
            prefsWriter.apply();

            return true;
        } else {
            return false;
        }
    }

    public List<Cookie> getCookies() {
        ArrayList<Cookie> ret = new ArrayList<>();
        for (String key : cookies.keySet()) {
            ret.addAll(cookies.get(key).values());
        }

        return ret;
    }

    /**
     * cookies 序列化成 string
     *
     * @param cookie 要序列化的cookie
     * @return 序列化之后的string
     */
    protected String encodeCookie(OkHttpCookies cookie) {
        if (cookie == null) {
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(os);
            outputStream.writeObject(cookie);
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException in encodeCookie", e);
            return null;
        }

        return byteArrayToHexString(os.toByteArray());
    }

    /**
     * 将字符串反序列化成cookies
     *
     * @param cookieString cookies string
     * @return cookie object
     */
    protected Cookie decodeCookie(String cookieString) {
        byte[] bytes = hexStringToByteArray(cookieString);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Cookie cookie = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            cookie = ((OkHttpCookies) objectInputStream.readObject()).getCookies();
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException in decodeCookie", e);
        } catch (ClassNotFoundException e) {
            Log.d(LOG_TAG, "ClassNotFoundException in decodeCookie", e);
        }

        return cookie;
    }

    /**
     * 二进制数组转十六进制字符串
     *
     * @param bytes byte array to be converted
     * @return string containing hex values
     */
    protected String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte element : bytes) {
            int v = element & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase(Locale.US);
    }

    /**
     * 十六进制字符串转二进制数组
     *
     * @param hexString string of hex-encoded values
     * @return decoded byte array
     */
    protected byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}

```
- （4）创建OkHttpClient时添加cookieJar
```
OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new LoginInterceptor())
                .cookieJar(new CookieManger (context))// 设置封装好的cookieJar
                .build();
```
###### 3、Retrofit，Gson解析，请求返回的类型不统一，假如double返回的是null
服务器返回类型不统一而引发的解析失败问题，开发中可能会遇到实体类定义的是某种类型（如double，int），但返回的是null或者字符串而解析失败
##### 实现目标
- 1、格式化数据不规范【格式化int类型数据】
- 2、格式化数据不规范【格式化Long类型数据】
- 3、格式化数据不规范【格式化Double类型数据】
- 4、格式化数据不规范【格式化String类型数据】
- 5、格式化数据不规范【格式化Null类型数据】
在上面已经创建好Retrofit并导入了相应的依赖包后，添加格式化工具方法（使用在.addConverterFactory(GsonConverterFactory.create(buildGson()))）

```

    /**
     * 增加后台返回""和"null"的处理,如果后台返回格式正常，此处不需要添加
     * 1.int=>0
     * 2.double=>0.00
     * 3.long=>0L
     * 4.String=>""
     *
     * @return
     */
    public Gson buildGson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapter(Integer.class, new IntegerDefaultAdapter())
                    .registerTypeAdapter(int.class, new IntegerDefaultAdapter())
                    .registerTypeAdapter(Double.class, new DoubleDefaultAdapter())
                    .registerTypeAdapter(double.class, new DoubleDefaultAdapter())
                    .registerTypeAdapter(Long.class, new LongDefaultAdapter())
                    .registerTypeAdapter(long.class, new LongDefaultAdapter())
                    .registerTypeAdapter(String.class, new StringNullAdapter())
                    .create();
        }
        return gson;
    }
```
对返回数据格式化处理
- 1.对double类型处理，返回“”，或“null”，动态更改为默认值0.00，新建DoubleDefaultAdapter类

```

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.gson
 * @Author: willkong
 * @CreateDate: 2019/7/18 12:55
 * @Description: 对返回值为空处理
 */
public class DoubleDefaultAdapter implements JsonSerializer<Double>, JsonDeserializer<Double> {
    @Override
    public Double deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            if (json.getAsString().equals("") || json.getAsString().equals("null")) {//定义为double类型,如果后台返回""或者null,则返回0.00
                return 0.00;
            }
        } catch (Exception ignore) {
        }
        try {
            return json.getAsDouble();
        } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
        }
    }

    @Override
    public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src);
    }
}
```
- 2.对int类型处理，返回“”，或“null”，动态更改为默认值0，新建IntegerDefaultAdapter类
```

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.gson
 * @Author: willkong
 * @CreateDate: 2019/7/18 12:57
 * @Description: 对返回值为空处理
 */
public class IntegerDefaultAdapter implements JsonSerializer<Integer>, JsonDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            if (json.getAsString().equals("") || json.getAsString().equals("null")) {//定义为int类型,如果后台返回""或者null,则返回0
                return 0;
            }
        } catch (Exception ignore) {
        }
        try {
            return json.getAsInt();
        } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
        }
    }

    @Override
    public JsonElement serialize(Integer src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src);
    }
}
```
- 3.对Long类型处理，返回“”，或“null”，动态更改为默认值0，新建LongDefaultAdapter类
```

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.gson
 * @Author: willkong
 * @CreateDate: 2019/7/18 12:58
 * @Description: 对返回值为空处理
 */

public class LongDefaultAdapter implements JsonSerializer<Long>, JsonDeserializer<Long> {
    @Override
    public Long deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            if (json.getAsString().equals("") || json.getAsString().equals("null")) {//定义为long类型,如果后台返回""或者null,则返回0
                return 0l;
            }
        } catch (Exception ignore) {
        }
        try {
            return json.getAsLong();
        } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
        }
    }

    @Override
    public JsonElement serialize(Long src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src);
    }
}
```

```
        retrofit = new Retrofit.Builder()
                .baseUrl(BaseContent.baseUrl)
                .addConverterFactory(GsonConverterFactory.create(buildGson()))//添加json转换框架(正常转换框架)
//                .addConverterFactory(MyGsonConverterFactory.create(buildGson()))//添加json自定义（根据需求，此种方法是拦截gson解析所做操作）
                //支持RxJava2
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(httpClientBuilder.build())
                .build();
```
###### 4、请求参数日志打印
**1.第一种办法，依赖第三方库**

```
compile 'com.squareup.okhttp3:logging-interceptor:3.11.0'
```
配置信息如下

```
OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
        if(BuildConfig.DEBUG){
            //显示日志
            logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        }else {
            logInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        httpClientBuilder.addInterceptor(logInterceptor);
```
**2.第二种办法，拦截器拦截（个人推荐第二种，可控性高）
给大家推荐一个打印日志库，很漂亮的日志结构**
在Build.gradle中添加依赖
```
 implementation 'com.orhanobut:logger:2.2.0'
```
设置我的日志打印样式如下：

```
  /**
     * 请求访问quest    打印日志
     * response拦截器
     */
    private Interceptor interceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startTime = System.currentTimeMillis();
            Response response = chain.proceed(chain.request());
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            MediaType mediaType = response.body().contentType();
            String content = response.body().string();

            Logger.wtf(TAG, "----------Request Start----------------");
            Logger.e(TAG, "| " + request.toString() + "===========" + request.headers().toString());
            Logger.json(content);
            Logger.e(content);
            Logger.wtf(TAG, "----------Request End:" + duration + "毫秒----------");

            return response.newBuilder()
                    .body(ResponseBody.create(mediaType, content))
                    .build();
        }
    };
```
然后在httpClientBuilder中添加拦截

```
OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder
                //打印日志拦截
                .addInterceptor(interceptor)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);//错误重联
```
###### 5、统一请求参数添加到请求头中

```
    /**
     * 添加  请求头
     */
    public class HeadUrlInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request()
                    .newBuilder()
//                    .addHeader("Content-Type", "text/html; charset=UTF-8")
//                    .addHeader("Vary", "Accept-Encoding")
//                    .addHeader("Server", "Apache")
//                    .addHeader("Pragma", "no-cache")
//                    .addHeader("Cookie", "add cookies here")
//                    .addHeader("_identity",  cookie_value)
                    .build();
            return chain.proceed(request);
        }
    }
```
然后在httpClientBuilder中添加拦截

```
OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder
                //添加参数到请求头
                .addInterceptor(new HeadUrlInterceptor())
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);//错误重联
```
###### 6、统一请求参数添加到请求body中

```

    /**
     * 获取HTTP 添加公共参数的拦截器
     * 暂时支持get、head请求&Post put patch的表单数据请求
     *
     * @return
     */
    public class HttpParamsInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            if (request.method().equalsIgnoreCase("GET") || request.method().equalsIgnoreCase("HEAD")) {
                HttpUrl httpUrl = request.url().newBuilder()
                        .addQueryParameter("version", "1.1.0")
                        .addQueryParameter("devices", "android")
                        .build();
                request = request.newBuilder().url(httpUrl).build();
            } else {
                RequestBody originalBody = request.body();
                if (originalBody instanceof FormBody) {
                    FormBody.Builder builder = new FormBody.Builder();
                    FormBody formBody = (FormBody) originalBody;
                    for (int i = 0; i < formBody.size(); i++) {
                        builder.addEncoded(formBody.encodedName(i), formBody.encodedValue(i));
                    }
                    FormBody newFormBody = builder
                            .addEncoded("version", "1.1.0")
                            .addEncoded("devices", "android")
                            .build();
                    if (request.method().equalsIgnoreCase("POST")) {
                        request = request.newBuilder().post(newFormBody).build();
                    } else if (request.method().equalsIgnoreCase("PATCH")) {
                        request = request.newBuilder().patch(newFormBody).build();
                    } else if (request.method().equalsIgnoreCase("PUT")) {
                        request = request.newBuilder().put(newFormBody).build();
                    }

                } else if (originalBody instanceof MultipartBody) {

                }

            }
            return chain.proceed(request);
        }
    }
```
然后在httpClientBuilder中添加拦截

```
OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder
                //添加参数到请求body
                .addInterceptor(new HttpParamsInterceptor())
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);//错误重联
```
###### 7、缓存的拦截器

```
  /**
     * 获得HTTP 缓存的拦截器
     *
     * @return
     */
    public class HttpCacheInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            // 无网络时，始终使用本地Cache
            if (!NetWorkUtils.isConnected()) {
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();
            }
            Response response = chain.proceed(request);
            if (NetWorkUtils.isConnected()) {
                //有网的时候读接口上的@Headers里的配置，你可以在这里进行统一的设置
                String cacheControl = request.cacheControl().toString();
                return response.newBuilder()
                        .header("Cache-Control", cacheControl)
                        .removeHeader("Pragma")
                        .build();
            } else {
                // 无网络时，设置超时为4周
                int maxStale = 60 * 60 * 24 * 28;
                return response.newBuilder()
                        //这里的设置的是我们的没有网络的缓存时间，想设置多少就是多少。
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .removeHeader("Pragma")
                        .build();
            }
        }
    }
```
然后在httpClientBuilder中添加拦截

```
 OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder
                .addInterceptor(new HttpCacheInterceptor())
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);//错误重联
```
###### 8、BaseUrl动态切换

用了一个博客中民间大神的拦截动态替换baseUrl方法有点问题，我暂时用了一种简单粗暴方法

```
@FormUrlEncoded
    @POST("http://www.baidu.com/api/user/edit?")
    Observable<BaseModel<Object>> getEditInfo(@FieldMap HashMap<String, String> params);
```
上边的路径是我随便写的，post中写全路径，这个优先级最高，同时设置了baseUrl不受影响
给大家一个专门写动态替换baseUrl连接 传送门
https://www.jianshu.com/p/2919bdb8d09a

###### 9、拦截指定接口，动态更改返回值便于测试

有时候我们需要返回指定值测试，可能需要空或者null等，迫于无法修改服务器返回数据，也没必要让后台修改数据，所以引发一个问题，如果拦截返回内容并修改指定字段值


```
 /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    /**
     * 特殊返回内容  处理方案
     */
    public class MockInterceptor implements Interceptor{
        @Override
        public Response intercept(Chain chain) throws IOException {
            Gson gson = new Gson();
            Response response = null;
            Response.Builder responseBuilder = new Response.Builder()
                    .code(200)
                    .message("")
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_0)
                    .addHeader("content-type", "application/json");
            Request request = chain.request();
            if(request.url().toString().contains(BaseContent.baseUrl)) { //拦截指定地址
                String responseString = "{\n" +
                        "\t\"success\": true,\n" +
                        "\t\"data\": [{\n" +
                        "\t\t\"id\": 6,\n" +
                        "\t\t\"type\": 2,\n" +
                        "\t\t\"station_id\": 1,\n" +
                        "\t\t\"datatime\": 1559491200000,\n" +
                        "\t\t\"factors\": [{\n" +
                        "\t\t\t\"id\": 11,\n" +
                        "\t\t\t\"history_id\": 6,\n" +
                        "\t\t\t\"station_id\": 1,\n" +
                        "\t\t\t\"factor_id\": 6,\n" +
                        "\t\t\t\"datatime\": 1559491200000,\n" +
                        "\t\t\t\"value_check\": 2.225,\n" +
                        "\t\t\t\"value_span\": 5.0,\n" +
                        "\t\t\t\"value_standard\": 4.0,\n" +
                        "\t\t\t\"error_difference\": -1.775,\n" +
                        "\t\t\t\"error_percent\": -44.38,\n" +
                        "\t\t\t\"accept\": false\n" +
                        "\t\t}, {\n" +
                        "\t\t\t\"id\": 12,\n" +
                        "\t\t\t\"history_id\": 6,\n" +
                        "\t\t\t\"station_id\": 1,\n" +
                        "\t\t\t\"factor_id\": 7,\n" +
                        "\t\t\t\"datatime\": 1559491200000,\n" +
                        "\t\t\t\"value_check\": 1.595,\n" +
                        "\t\t\t\"value_span\": 0.5,\n" +
                        "\t\t\t\"value_standard\": 1.6,\n" +
                        "\t\t\t\"error_difference\": -0.005,\n" +
                        "\t\t\t\"error_percent\": -0.31,\n" +
                        "\t\t\t\"accept\": true\n" +
                        "\t\t}, {\n" +
                        "\t\t\t\"id\": 13,\n" +
                        "\t\t\t\"history_id\": 6,\n" +
                        "\t\t\t\"station_id\": 1,\n" +
                        "\t\t\t\"factor_id\": 8,\n" +
                        "\t\t\t\"datatime\": 1559491200000,\n" +
                        "\t\t\t\"value_check\": 8.104,\n" +
                        "\t\t\t\"value_span\": 20.0,\n" +
                        "\t\t\t\"value_standard\": 8.0,\n" +
                        "\t\t\t\"error_difference\": 0.104,\n" +
                        "\t\t\t\"error_percent\": 1.3,\n" +
                        "\t\t\t\"accept\": true\n" +
                        "\t\t},null]\n" +
                        "\t}],\n" +
                        "\t\"additional_data\": {\n" +
                        "\t\t\"totalPage\": 0,\n" +
                        "\t\t\"startPage\": 1,\n" +
                        "\t\t\"limit\": 30,\n" +
                        "\t\t\"total\": 0,\n" +
                        "\t\t\"more_items_in_collection\": false\n" +
                        "\t},\n" +
                        "\t\"related_objects\": [{\n" +
                        "\t\t\"id\": 6,\n" +
                        "\t\t\"name\": \"氨氮\",\n" +
                        "\t\t\"unit\": \"mg/L\",\n" +
                        "\t\t\"db_field\": \"nh3n\",\n" +
                        "\t\t\"qa_ratio\": true\n" +
                        "\t}, {\n" +
                        "\t\t\"id\": 7,\n" +
                        "\t\t\"name\": \"总磷\",\n" +
                        "\t\t\"unit\": \"mg/L\",\n" +
                        "\t\t\"db_field\": \"tp\",\n" +
                        "\t\t\"qa_ratio\": true\n" +
                        "\t}, {\n" +
                        "\t\t\"id\": 8,\n" +
                        "\t\t\"name\": \"总氮\",\n" +
                        "\t\t\"unit\": \"mg/L\",\n" +
                        "\t\t\"db_field\": \"tn\",\n" +
                        "\t\t\"qa_ratio\": true\n" +
                        "\t}, {\n" +
                        "\t\t\"id\": 9,\n" +
                        "\t\t\"name\": \"CODMn\",\n" +
                        "\t\t\"unit\": \"mg/L\",\n" +
                        "\t\t\"db_field\": \"codmn\",\n" +
                        "\t\t\"qa_ratio\": true\n" +
                        "\t}],\n" +
                        "\t\"request_time\": \"2019-06-05T16:40:14.915+08:00\"\n" +
                        "}";
                responseBuilder.body(ResponseBody.create(MediaType.parse("application/json"), responseString.getBytes()));//将数据设置到body中
                response = responseBuilder.build(); //builder模式构建response
            }else{
                response = chain.proceed(request);
            }
            return response;
        }
    }
```
然后在httpClientBuilder中添加拦截


```
OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder
                .addInterceptor(new MockInterceptor())
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);//错误重联
```
###### 10.处理一些识别识别不了 ipv6手机，如小米  实现方案  将ipv6与ipv4置换位置，首先用ipv4解析

新建一个类命名为ApiDns如下：

```

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Dns;

/**
 * File descripition:   ipv6和ipv4换位  处理部分手机网络请求慢问题
 *
 * @author lp
 * @date 2019/4/16
 */

public class ApiDns implements Dns {
    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (hostname == null) {
            throw new UnknownHostException("hostname == null");
        } else {
            try {
                List<InetAddress> mInetAddressesList = new ArrayList<>();
                InetAddress[] mInetAddresses = InetAddress.getAllByName(hostname);
                for (InetAddress address : mInetAddresses) {
                    if (address instanceof Inet4Address) {
                        mInetAddressesList.add(0, address);
                    } else {
                        mInetAddressesList.add(address);
                    }
                }
                return mInetAddressesList;
            } catch (NullPointerException var4) {
                UnknownHostException unknownHostException = new UnknownHostException("Broken system behaviour");
                unknownHostException.initCause(var4);
                throw unknownHostException;
            }
        }
    }
}
```
然后在httpClientBuilder中添加dns设置

```
 OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder
                .dns(new ApiDns())
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);//错误重联
```
创建一个api接口类,命名为ApiServer

```

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.api
 * @Author: willkong
 * @CreateDate: 2019/7/25 16:25
 * @Description: api接口类
 */

public interface ApiServer {

    /**
     * 第一种写法
     *
     * @param requestType
     * @return
     */
    @POST("api/Activity/get_activities?")
    Observable<BaseModel<List<MainBean>>> getMain(@Query("time") String requestType);

    /**
     * 第二种写法
     *
     * @param requestType
     * @return
     */
    @FormUrlEncoded
    @POST("api/Activity/get_activities?")
    Observable<BaseModel<List<MainBean>>> getMain2(@Field("time") String requestType);

    /**
     * 第三种写法
     *
     * @param params
     * @return
     */
    @FormUrlEncoded
    @POST("api/Activity/get_activities?")
    Observable<BaseModel<HashMap<String, String>>> getMain3(@FieldMap HashMap<String, String> params);

    /**
     * 演示 单图上传
     *
     * @param parts
     * @return
     */
    @Multipart
    @POST("api/Company/register")
    Observable<BaseModel<Object>> upLoadImg(@Part MultipartBody.Part parts);

    /**
     * 演示 多图上传
     *
     * @param parts
     * @return
     */
    @Multipart
    @POST("api/user_info/update_headimg")
    Observable<BaseModel<Object>> upHeadImg(@Part List<MultipartBody.Part> parts);

    /**
     * 演示 图片字段一起上传
     *
     * @param parts
     * @return
     */
    @Multipart
    @POST("api/Express/add")
    Observable<BaseModel<Object>> expressAdd(@PartMap Map<String, RequestBody> map,
                                             @Part List<MultipartBody.Part> parts);



    /**
     * 演示特殊结构写法
     *
     * @param requestType
     * @return
     */
    @POST("api/Activity/get_activities?")
    Observable<com.lp.mvp_network.second2demo.mvp.BaseModel<List<Bean1>, Bean2, List<Bean3>>> getMain2Demo(@Query("time") String requestType);
}

```
ApiRetrofit类中创建接口api对象，并绑定到Retrofit
使用如下：

```
private static ApiRetrofit apiRetrofit;
apiServer = retrofit.create(ApiServer.class);
```
最后ApiRetrofit类的完整代码如下：

```

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orhanobut.logger.Logger;
import com.willkong.mvp_network.App;
import com.willkong.mvp_network.base.BaseContent;
import com.willkong.mvp_network.base.cookie.CookieManger;
import com.willkong.mvp_network.base.gson.DoubleDefaultAdapter;
import com.willkong.mvp_network.base.gson.IntegerDefaultAdapter;
import com.willkong.mvp_network.base.gson.LongDefaultAdapter;
import com.willkong.mvp_network.base.gson.StringNullAdapter;
import com.willkong.mvp_network.utils.NetWorkUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static okhttp3.internal.Util.UTF_8;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.api
 * @Author: willkong
 * @CreateDate: 2019/7/18 11:22
 * @Description: Retrofit的配置封装
 */
public class ApiRetrofit {
    private String TAG = "ApiRetrofit %s";
    private ApiServer apiServer;
    private static ApiRetrofit apiRetrofit;
    /**
     * Retrofit网络请求对象
     */
    private Retrofit retrofit;

    /**
     * gosn格式化处理
     */
    private Gson gson;
    /**
     * 网络超时时间
     */
    private static final int DEFAULT_TIMEOUT = 15;

    /**
     * 网络请求类需要设置为单例模式
     *
     * @return
     */
    public static ApiRetrofit getInstance() {
        if (apiRetrofit == null) {
            synchronized (Object.class) {
                if (apiRetrofit == null) {
                    apiRetrofit = new ApiRetrofit();
                }
            }
        }
        return apiRetrofit;
    }

    /**
     * ApiRetrofit的构造函数
     * 配置网络
     */
    public ApiRetrofit() {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder
                .cookieJar(new CookieManger(App.instance()))
                .addInterceptor(interceptor)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);//错误重联


        /**
         * 处理一些识别识别不了 ipv6手机，如小米  实现方案  将ipv6与ipv4置换位置，首先用ipv4解析
         */
        httpClientBuilder.dns(new ApiDns());

        /**
         * 添加cookie管理
         * 方法1：第三方框架
         */
        PersistentCookieJar cookieJar = new PersistentCookieJar(new SetCookieCache(),
                new SharedPrefsCookiePersistor(App.instance()));
        httpClientBuilder.cookieJar(cookieJar);

        /**
         * 添加cookie管理
         * 方法2：手动封装cookie管理
         */
//        httpClientBuilder.cookieJar(new CookieManger(App.instance()));

        /**
         * 添加日志拦截
         */
        httpClientBuilder.addInterceptor(interceptor);
        /**
         * 添加请求头
         */
        httpClientBuilder.addInterceptor(new HeadUrlInterceptor());

        retrofit = new Retrofit.Builder()
                .baseUrl(BaseContent.baseUrl)
                .addConverterFactory(GsonConverterFactory.create(buildGson()))//添加json转换框架(正常转换框架)
//                .addConverterFactory(MyGsonConverterFactory.create(buildGson()))//添加json自定义（根据需求，此种方法是拦截gson解析所做操作）
                //支持RxJava2
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(httpClientBuilder.build())
                .build();
//
        apiServer = retrofit.create(ApiServer.class);
    }

    /**
     * 增加后台返回""和"null"的处理,如果后台返回格式正常，此处不需要添加
     * 1.int=>0
     * 2.double=>0.00
     * 3.long=>0L
     * 4.String=>""
     *
     * @return
     */
    public Gson buildGson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapter(Integer.class, new IntegerDefaultAdapter())
                    .registerTypeAdapter(int.class, new IntegerDefaultAdapter())
                    .registerTypeAdapter(Double.class, new DoubleDefaultAdapter())
                    .registerTypeAdapter(double.class, new DoubleDefaultAdapter())
                    .registerTypeAdapter(Long.class, new LongDefaultAdapter())
                    .registerTypeAdapter(long.class, new LongDefaultAdapter())
                    .registerTypeAdapter(String.class, new StringNullAdapter())
                    .create();
        }
        return gson;
    }

    /**
     * 请求访问quest    打印日志
     * response拦截器
     */
    private Interceptor interceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startTime = System.currentTimeMillis();
            Response response = chain.proceed(chain.request());
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            MediaType mediaType = response.body().contentType();
            String content = response.body().string();

            Logger.wtf(TAG, "----------Request Start----------------");
            printParams(request.body());
            Logger.e(TAG, "| " + request.toString() + "===========" + request.headers().toString());
            Logger.json(content);
            Logger.e(content);
            Logger.wtf(TAG, "----------Request End:" + duration + "毫秒----------");

            return response.newBuilder()
                    .body(ResponseBody.create(mediaType, content))
                    .build();
        }
    };

    /**
     * 请求参数日志打印
     *
     * @param body
     */
    private void printParams(RequestBody body) {
        if (body != null) {
            Buffer buffer = new Buffer();
            try {
                body.writeTo(buffer);
                Charset charset = Charset.forName("UTF-8");
                MediaType contentType = body.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF_8);
                }
                String params = buffer.readString(charset);
                Logger.e(TAG, "请求参数： | " + params);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加  请求头
     */
    public class HeadUrlInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request()
                    .newBuilder()
//                    .addHeader("Content-Type", "text/html; charset=UTF-8")
//                    .addHeader("Vary", "Accept-Encoding")
//                    .addHeader("Server", "Apache")
//                    .addHeader("Pragma", "no-cache")
//                    .addHeader("Cookie", "add cookies here")
//                    .addHeader("_identity",  cookie_value)
                    .build();
            return chain.proceed(request);
        }
    }


    /**
     * 获取HTTP 添加公共参数的拦截器
     * 暂时支持get、head请求&Post put patch的表单数据请求
     *
     * @return
     */
    public class HttpParamsInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            if (request.method().equalsIgnoreCase("GET") || request.method().equalsIgnoreCase("HEAD")) {
                HttpUrl httpUrl = request.url().newBuilder()
                        .addQueryParameter("version", "1.1.0")
                        .addQueryParameter("devices", "android")
                        .build();
                request = request.newBuilder().url(httpUrl).build();
            } else {
                RequestBody originalBody = request.body();
                if (originalBody instanceof FormBody) {
                    FormBody.Builder builder = new FormBody.Builder();
                    FormBody formBody = (FormBody) originalBody;
                    for (int i = 0; i < formBody.size(); i++) {
                        builder.addEncoded(formBody.encodedName(i), formBody.encodedValue(i));
                    }
                    FormBody newFormBody = builder
                            .addEncoded("version", "1.1.0")
                            .addEncoded("devices", "android")
                            .build();
                    if (request.method().equalsIgnoreCase("POST")) {
                        request = request.newBuilder().post(newFormBody).build();
                    } else if (request.method().equalsIgnoreCase("PATCH")) {
                        request = request.newBuilder().patch(newFormBody).build();
                    } else if (request.method().equalsIgnoreCase("PUT")) {
                        request = request.newBuilder().put(newFormBody).build();
                    }

                } else if (originalBody instanceof MultipartBody) {

                }
            }
            return chain.proceed(request);
        }
    }

    /**
     * 获得HTTP 缓存的拦截器
     *
     * @return
     */
    public class HttpCacheInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            // 无网络时，始终使用本地Cache
            if (!NetWorkUtils.isConnected()) {
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();
            }
            Response response = chain.proceed(request);
            if (NetWorkUtils.isConnected()) {
                //有网的时候读接口上的@Headers里的配置，你可以在这里进行统一的设置
                String cacheControl = request.cacheControl().toString();
                return response.newBuilder()
                        .header("Cache-Control", cacheControl)
                        .removeHeader("Pragma")
                        .build();
            } else {
                // 无网络时，设置超时为4周
                int maxStale = 60 * 60 * 24 * 28;
                return response.newBuilder()
                        //这里的设置的是我们的没有网络的缓存时间，想设置多少就是多少。
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .removeHeader("Pragma")
                        .build();
            }
        }
    }

    /**
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     */

    /**
     * 特殊返回内容  处理方案
     */
    public class MockInterceptor implements Interceptor{
        @Override
        public Response intercept(Chain chain) throws IOException {
            Gson gson = new Gson();
            Response response = null;
            Response.Builder responseBuilder = new Response.Builder()
                    .code(200)
                    .message("")
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_0)
                    .addHeader("content-type", "application/json");
            Request request = chain.request();
            if(request.url().toString().contains(BaseContent.baseUrl)) { //拦截指定地址
                String responseString = "{\n" +
                        "\t\"success\": true,\n" +
                        "\t\"data\": [{\n" +
                        "\t\t\"id\": 6,\n" +
                        "\t\t\"type\": 2,\n" +
                        "\t\t\"station_id\": 1,\n" +
                        "\t\t\"datatime\": 1559491200000,\n" +
                        "\t\t\"factors\": [{\n" +
                        "\t\t\t\"id\": 11,\n" +
                        "\t\t\t\"history_id\": 6,\n" +
                        "\t\t\t\"station_id\": 1,\n" +
                        "\t\t\t\"factor_id\": 6,\n" +
                        "\t\t\t\"datatime\": 1559491200000,\n" +
                        "\t\t\t\"value_check\": 2.225,\n" +
                        "\t\t\t\"value_span\": 5.0,\n" +
                        "\t\t\t\"value_standard\": 4.0,\n" +
                        "\t\t\t\"error_difference\": -1.775,\n" +
                        "\t\t\t\"error_percent\": -44.38,\n" +
                        "\t\t\t\"accept\": false\n" +
                        "\t\t}, {\n" +
                        "\t\t\t\"id\": 12,\n" +
                        "\t\t\t\"history_id\": 6,\n" +
                        "\t\t\t\"station_id\": 1,\n" +
                        "\t\t\t\"factor_id\": 7,\n" +
                        "\t\t\t\"datatime\": 1559491200000,\n" +
                        "\t\t\t\"value_check\": 1.595,\n" +
                        "\t\t\t\"value_span\": 0.5,\n" +
                        "\t\t\t\"value_standard\": 1.6,\n" +
                        "\t\t\t\"error_difference\": -0.005,\n" +
                        "\t\t\t\"error_percent\": -0.31,\n" +
                        "\t\t\t\"accept\": true\n" +
                        "\t\t}, {\n" +
                        "\t\t\t\"id\": 13,\n" +
                        "\t\t\t\"history_id\": 6,\n" +
                        "\t\t\t\"station_id\": 1,\n" +
                        "\t\t\t\"factor_id\": 8,\n" +
                        "\t\t\t\"datatime\": 1559491200000,\n" +
                        "\t\t\t\"value_check\": 8.104,\n" +
                        "\t\t\t\"value_span\": 20.0,\n" +
                        "\t\t\t\"value_standard\": 8.0,\n" +
                        "\t\t\t\"error_difference\": 0.104,\n" +
                        "\t\t\t\"error_percent\": 1.3,\n" +
                        "\t\t\t\"accept\": true\n" +
                        "\t\t},null]\n" +
                        "\t}],\n" +
                        "\t\"additional_data\": {\n" +
                        "\t\t\"totalPage\": 0,\n" +
                        "\t\t\"startPage\": 1,\n" +
                        "\t\t\"limit\": 30,\n" +
                        "\t\t\"total\": 0,\n" +
                        "\t\t\"more_items_in_collection\": false\n" +
                        "\t},\n" +
                        "\t\"related_objects\": [{\n" +
                        "\t\t\"id\": 6,\n" +
                        "\t\t\"name\": \"氨氮\",\n" +
                        "\t\t\"unit\": \"mg/L\",\n" +
                        "\t\t\"db_field\": \"nh3n\",\n" +
                        "\t\t\"qa_ratio\": true\n" +
                        "\t}, {\n" +
                        "\t\t\"id\": 7,\n" +
                        "\t\t\"name\": \"总磷\",\n" +
                        "\t\t\"unit\": \"mg/L\",\n" +
                        "\t\t\"db_field\": \"tp\",\n" +
                        "\t\t\"qa_ratio\": true\n" +
                        "\t}, {\n" +
                        "\t\t\"id\": 8,\n" +
                        "\t\t\"name\": \"总氮\",\n" +
                        "\t\t\"unit\": \"mg/L\",\n" +
                        "\t\t\"db_field\": \"tn\",\n" +
                        "\t\t\"qa_ratio\": true\n" +
                        "\t}, {\n" +
                        "\t\t\"id\": 9,\n" +
                        "\t\t\"name\": \"CODMn\",\n" +
                        "\t\t\"unit\": \"mg/L\",\n" +
                        "\t\t\"db_field\": \"codmn\",\n" +
                        "\t\t\"qa_ratio\": true\n" +
                        "\t}],\n" +
                        "\t\"request_time\": \"2019-06-05T16:40:14.915+08:00\"\n" +
                        "}";
                responseBuilder.body(ResponseBody.create(MediaType.parse("application/json"), responseString.getBytes()));//将数据设置到body中
                response = responseBuilder.build(); //builder模式构建response
            }else{
                response = chain.proceed(request);
            }
            return response;
        }
    }
}

```
## Retrofit，Gson解析，自定义解析内容（如code=1全部解析，code=0不做解析）
开发中可能会遇到特殊情况，接口返回正常标识（如code=1），我们用GSON自动化解析全部内容。接口返回异常标识（如code=1001），可能会出现返回的类型不统一，原本是对象内容，但是返回了数组，原本是数组，返回的是对象，导致GSON自动化解析失败，在不改后台代码的情况下，**前端处理**，解决办法请往下看
解决办法如下
- 1、在上述创建retrofit时，Retrofit需要一个指定转换框架，如fastJson，GSON等，我们采取GSON转化，为了便于我们的需求，需要重写一下GSON源码--修改成我们自己重写的GsonConverterFactory
原始的GsonConverterFactory

```
.addConverterFactory(GsonConverterFactory.create())
```
改成如下我们重写的MyGsonConverterFactory

```
.addConverterFactory(MyGsonConverterFactory.create())
```
总共需要重写三个类
第一个类为：MyGsonConverterFactory源码如下（以下内容复制粘贴即可，和源码一样）

```

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * File descripition: 重写gson 判断返回值  状态划分
 *
 * A {@linkplain Converter.Factory converter} which uses Gson for JSON.
 * <p>
 * Because Gson is so flexible in the types it supports, this converter assumes that it can handle
 * all types. If you are mixing JSON serialization with something else (such as protocol buffers),
 * you must {@linkplain Retrofit.Builder#addConverterFactory(Converter.Factory) add this instance}
 * last to allow the other converters a chance to see their types.
 */
public final class MyGsonConverterFactory extends Converter.Factory {
    /**
     * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
     * decoding from JSON (when no charset is specified by a header) will use UTF-8.
     */
    public static MyGsonConverterFactory create() {
        return create(new Gson());
    }

    /**
     * Create an instance using {@code gson} for conversion. Encoding to JSON and
     * decoding from JSON (when no charset is specified by a header) will use UTF-8.
     */
    @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
    public static MyGsonConverterFactory create(Gson gson) {
        if (gson == null) throw new NullPointerException("gson == null");
        return new MyGsonConverterFactory(gson);
    }

    private final Gson gson;

    private MyGsonConverterFactory(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
                                                            Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new MyGsonResponseBodyConverter<>(gson, adapter);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type,
                                                          Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new MyGsonRequestBodyConverter<>(gson, adapter);
    }
}

```
上述的类中，使用到了两个类MyGsonResponseBodyConverter和MyGsonRequestBodyConverter

第二个类为：MyGsonRequestBodyConverter源码如下（以下内容复制粘贴即可，和源码一样）

```


import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

/**
 * File descripition:   重写gson 判断返回值  状态划分
 */

final class MyGsonRequestBodyConverter<T> implements Converter<T, RequestBody> {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Gson gson;
    private final TypeAdapter<T> adapter;

    MyGsonRequestBodyConverter(Gson gson, TypeAdapter<T> adapter) {
        this.gson = gson;
        this.adapter = adapter;
    }

    @Override
    public RequestBody convert(T value) throws IOException {
        Buffer buffer = new Buffer();
        Writer writer = new OutputStreamWriter(buffer.outputStream(), UTF_8);
        JsonWriter jsonWriter = gson.newJsonWriter(writer);
        adapter.write(jsonWriter, value);
        jsonWriter.close();
        return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
    }
}
```
**第三个类为：MyGsonResponseBodyConverter源码如下（此类需要自定义内容）**
说明：convert函数中包含服务器请求下全部内容，我们单独拿出下发的code，message字段，用于状态标识，解析方式有多种，用gson解析或者强转都可以

```
BaseResult re = gson.fromJson(response, BaseResult.class);
```
或者

```
BaseResult re  = (BaseResult) response;
```
拿到code时，如果code和约定正常值不同（如code=1），余下内容没必要进行解析，非1情况一律抛出ApiException异常，转交给onError（）处理

```

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.willkong.mvp_network.base.ApiException;
import com.willkong.mvp_network.base.BaseContent;
import com.willkong.mvp_network.base.model.BaseModel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Converter;

import static okhttp3.internal.Util.UTF_8;


/**
 * File descripition:   重写gson 判断返回值  状态划分
 * <p>
 * 此处很重要
 * 为何这样写：因为开发中有这样的需求   当服务器返回假如0是正常 1是不正常  0我们gson 或 fastJson解析数据
 * 1我们不想解析（可能返回值出现以前是对象 数据为空变成了数组等等，于是在不改后台代码的情况下  我们前端需要处理）
 * 但是用了插件之后没有很有效的方法控制解析 所以处理方式为  当服务器返回不等于0时候  其他状态都抛出异常 然后提示
 * <p>
 * <p>
 * 此处为如果在解析这一步拦截  可采取这种方式
 */

final class MyGsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final Gson gson;
    private final TypeAdapter<T> adapter;

    MyGsonResponseBodyConverter(Gson gson, TypeAdapter<T> adapter) {
        this.gson = gson;
        this.adapter = adapter;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        String response = value.string();
        BaseModel re = gson.fromJson(response, BaseModel.class);
        //关注的重点，自定义响应码中非0的情况，一律抛出ApiException异常。
        //这样，我们就成功的将该异常交给onError()去处理了。
        if (re.getErrcode() != BaseContent.basecode) {
            value.close();
            throw new ApiException(re.getErrcode(), re.getErrmsg());
        }

        MediaType mediaType = value.contentType();
        Charset charset = mediaType != null ? mediaType.charset(UTF_8) : UTF_8;
        ByteArrayInputStream bis = new ByteArrayInputStream(response.getBytes());
        InputStreamReader reader = new InputStreamReader(bis, charset);
        JsonReader jsonReader = gson.newJsonReader(reader);
        try {
            return adapter.read(jsonReader);
        } finally {
            value.close();
        }
    }
}

```
再说明一下上边实现逻辑，期间我们拦截解析一步，先定义一个BaseResult，实体类只含code,和message，解析这个实体类内容，拿到code返回值，与后台比对约定好的状态码（比如1是正常），当返回1，我们不做处理，继续向下解析，如果返回其他值，如0或1001等，我们一律抛出自定义异常处理实体类ApiException，将code和message放进去，到异常里进行逻辑判断，下边贴一下BaseResult，ApiException这俩个类的代码

```
/**
 * File descripition:   状态划分 基类
 *
 */

public class BaseResult {
    public String message;
    public int code;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
```
**ApiException源码如下**

```
/**
 * File descripition:   异常处理基类
 */

public class ApiException extends RuntimeException {
    private int errorCode;

    public ApiException(int code, String msg) {
        super(msg);
        this.errorCode = code;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
```
上述已经把没有请求到正常数据的异常都抛出给了我们的异常类ApiException处理，我们只需要判断到不是跟后台约定好的成功码code,就走error接口。在error这里我们根据相应的code码来抛出相应的提示语。
## 开始分析抛出的异常内容
如果我们项目结构不同，可采取这种思路处理，下边分析解决办法

onError代表数据解析失败，如无网，http链接异常等等，首先需要通过异常类型来判断哪种异常并做对应提示

```
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
        } else {
            if (e != null) {
                onError(e.toString());
            } else {
                onError("未知错误");
            }
        }
    }
```
基本异常分析完毕，再进行分析，通过上述内容可知，服务器返回非1（code=1）情况数据内容都会在onError中（ApiException异常类），所以，我们需要将ApiException提出来得知是哪种异常情况
首先判断异常信息是否包含ApiException

```
e instanceof ApiException
```
然后继续向下分析，并分发异常处理

```
if (e instanceof ApiException) {
        ApiException exception = (ApiException) e;
        int code = exception.getErrorCode();
        switch (code) {
         //未登录（这块只是提供个演示）
        case CONNECT_NOT_LOGIN:
             view.onErrorCode(new BaseModel(exception.getMessage(), code));
             onException(CONNECT_NOT_LOGIN, exception.getMessage());
             break;
        //其他不等于0 的所有状态
         default:
             onException(OTHER_MESSAGE, exception.getMessage());
             view.onErrorCode(new BaseModel(exception.getMessage(), code));
             break;
       }
 }
```
#### 全部代码如下
```
import com.google.gson.JsonParseException;
import com.lp.mvp_network.base.ApiException;
import com.lp.mvp_network.base.BaseContent;
import com.lp.mvp_network.utils.NetWorkUtils;

import org.json.JSONException;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.ParseException;

import io.reactivex.observers.DisposableObserver;
import retrofit2.HttpException;

/**
 * File descripition:   数据处理基类
 *
 * @author lp
 * @date 2018/6/19
 */

public abstract class BaseObserver<T> extends DisposableObserver<T> {

    public static final int CODE = BaseContent.basecode;

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
     * data为null
     */
    public static final int CONNECT_NULL = 5555;
    /**
     * 未登录  与服务器约定返回的值   这里未登录只是一个案例
     */
    public static final int CONNECT_NOT_LOGIN = 1001;
    /**
     * 其他code  提示
     */
    public static final int OTHER_MESSAGE = 20000;


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
    public void onNext(T o) {
        try {
            if (view != null) {
                view.hideLoading();
            }

            Gson gson = new Gson();
            BaseModel model = gson.fromJson(o.toString(), BaseModel.class);
            if (model.getData() != null) {
                onSuccess(o);
            } else {
               if (view != null) {
                    model.setErrcode(CONNECT_NULL);
                    model.setErrmsg("怎么会是null呢");
                    view.onErrorCode(model);
                    onException(model.getErrcode(), model.getErrmsg());
                }
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
            switch (code) {
                //未登录（此处只是案例 供理解）
                case CONNECT_NOT_LOGIN:
                    view.onErrorCode(new BaseModel(exception.getMessage(), code));
                    onException(CONNECT_NOT_LOGIN, "");
                    break;
                //其他不等于0 的所有状态
                default:
                    onException(OTHER_MESSAGE, exception.getMessage());
                    view.onErrorCode(new BaseModel(exception.getMessage(), code));
                    break;
            }
        } else {
            if (e != null) {
                onError(e.toString());
            } else {
                onError("未知错误");
            }
        }

    }

    /**
     * 中间拦截一步  判断是否有网络  这步判断相对准确  此步去除也可以
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
            //未登录
            case CONNECT_NOT_LOGIN:
//                onError("未登录");
                break;
            //正常执行  提示信息
            case OTHER_MESSAGE:
                onError(message);
                break;
            //网络不可用
            case NETWORK_ERROR:
                onError("网络不可用，请检查网络连接！");
                break;
            //data==null
            case CONNECT_NULL:
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

    public abstract void onSuccess(T o);

    public abstract void onError(String msg);
}
```
如果做了自定义解析体了，也可以编写为如下

```

import com.google.gson.JsonParseException;
import com.willkong.mvp_network.base.ApiException;
import com.willkong.mvp_network.base.BaseContent;

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

```
至此 自定义解析体实现完成。
到这里Retrofit2+okhttp3+rxjava2的封装基本完成了，下面我们再加入MVP的架构。
1. 编写MVP的基类BaseView视图接口
代码如下：

```

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.mvp
 * @Author: willkong
 * @CreateDate: 2019/7/25 17:16
 * @Description: 基本回调 可自定义添加所需回调
 */

public interface BaseView {
    /**
     * 显示dialog
     */
    void showLoading();
    /**
     * 隐藏 dialog
     */

    void hideLoading();
    /**
     * 显示错误信息
     *
     * @param msg
     */
    void showError(String msg);
    /**
     * 后台返回错误码
     */
    void onErrorCode(BaseModel model);
}

```
2. 新建MVP的BasePresenter中间处理基类
完整代码如下：

```

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

```
实体基类

```

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
```
# 下面添加Retrofit文件上传（MVP模式下）
实现目标
- 1、单图上传
- 2、多图上传
- 3、图片参数混合上传

编写工具类RetrofitUtil 

```
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.utils
 * @Author: willkong
 * @CreateDate: 2019/7/25 17:50
 * @Description: Retrofit图片文字上传工具类
 */
public class RetrofitUtil {
    /**
     * 将String 字符串转换为Rrtorfit: requestBody类型的value
     */
    public static RequestBody convertToRequestBody(String param) {
        RequestBody requestBody = null;
        requestBody = RequestBody.create(MediaType.parse("text/plain"), param);
        return requestBody;
    }

    /**
     * 将所有的File图片集合转化为retorfit上传图片所需的： MultipartBody.Part类型的集合
     */
    public static List<MultipartBody.Part> filesToMultipartBodyParts(List<File> files, String key) {
        List<MultipartBody.Part> parts = new ArrayList<>(files.size());
        for (File file : files) {
            parts.add(filesToMultipartBodyParts(file, key));
        }
        return parts;
    }

    /**
     * 将单个File图片转化为retorfit上传图片所需的： MultipartBody.Part类型
     */
    public static MultipartBody.Part filesToMultipartBodyParts(File file, String key) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData(key, file.getName(), requestBody);
        return part;
    }

    /**
     * 将图片的路径集合转化为文件集合
     * @param mImages
     * @return
     */
    public static List<File> initImages(List<String> mImages) {
        List<File> listPicture = new ArrayList<>();
        listPicture.clear();
        Iterator<String> stuIter = mImages.iterator();
        while (stuIter.hasNext()) {
            String mUrl = stuIter.next();
            listPicture.add(new File(mUrl));
        }
        return listPicture;
    }
}
```
###### 1、单图上传
1.第一种方法
首先创建接口，注意注解需要用@Multipart 参数形式@Part MultipartBody.Part parts

```
@Multipart
@POST("api/Company/register")
Observable<BaseModel<Object>> upLoadImg(@Part MultipartBody.Part parts);
```
使用示例

```

public interface UpLoadView extends BaseView {
    void onUpLoadImgSuccess(BaseModel<Object> o);
}

public class UpLoadPresenter extends BasePresenter<UpLoadView> {
    public UpLoadPresenter(UpLoadView baseView) {
        super(baseView);
    }

    public void upLoadImgApi(MultipartBody.Part parts) {
        addDisposable(apiServer.upLoadImg(parts), new BaseObserver(baseView) {
            @Override
            public void onSuccess(Object o) {
                baseView.onUpLoadImgSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }
}

 @Override
 public void onClick(View v) {
        /**
         * 俩个参数  一个是图片路径   一个是和后台约定的Key，如果后台不需要，随便写都行
         */
        mPresenter.upLoadImgApi(RetrofitUtil.filesToMultipartBodyParts(new File("tupian.lujing"), "tupian.key"));
    }
```
2.第二种方法
首先创建接口，注意注解需要用@Multipart 参数形式List<MultipartBody.Part> parts
问：为啥用List? 答：List只有一张图片不就是单张了 可以冷笑一下不介意

```
@Multipart
@POST("api/user_info/update_headimg")
Observable<BaseModel<Object>> upHeadImg(@Part List<MultipartBody.Part> parts);
```

```
public interface UpLoadView extends BaseView {
    void onUpHeadImgSuccess(BaseModel<Object> o);
}

public class UpLoadPresenter extends BasePresenter<UpLoadView> {
    public UpLoadPresenter(UpLoadView baseView) {
        super(baseView);
    }

    public void upHeadImgApi(List<MultipartBody.Part> parts) {
        addDisposable(apiServer.upHeadImg(parts), new BaseObserver(baseView) {
            @Override
            public void onSuccess(Object o) {
                baseView.onUpHeadImgSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }
}
@Override
public void onClick(View v) {
    /**
     * 俩个参数  一个是图片集合路径   一个是和后台约定的Key，如果后台不需要，随便写都行
     */
  List<String> strings=new ArrayList<>();
    for (int i=0;i<1;i++){
        strings.add("tupian.lujing");
    }
    mPresenter.upHeadImgApi(RetrofitUtil.filesToMultipartBodyParts(RetrofitUtil.initImages(strings), "tupian.key"));
}
```
###### 2、多图上传
和单图上传第二种方法一样

```
@Multipart
@POST("api/user_info/update_headimg")
Observable<BaseModel<Object>> upHeadImg(@Part List<MultipartBody.Part> parts);
```

```
public interface UpLoadView extends BaseView {
    void onUpHeadImgSuccess(BaseModel<Object> o);
}

public class UpLoadPresenter extends BasePresenter<UpLoadView> {
    public UpLoadPresenter(UpLoadView baseView) {
        super(baseView);
    }

    public void upHeadImgApi(List<MultipartBody.Part> parts) {
        addDisposable(apiServer.upHeadImg(parts), new BaseObserver(baseView) {
            @Override
            public void onSuccess(Object o) {
                baseView.onUpHeadImgSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }
}
@Override
public void onClick(View v) {
    /**
     * 俩个参数  一个是图片集合路径   一个是和后台约定的Key，如果后台不需要，随便写都行
     */
  List<String> strings=new ArrayList<>();
    for (int i=0;i<100;i++){
        strings.add("tupian.lujing");
    }
    mPresenter.upHeadImgApi(RetrofitUtil.filesToMultipartBodyParts(RetrofitUtil.initImages(strings), "tupian.key"));
}
```
###### 3、图片参数混合上传
首先创建接口，注意注解需要用@Multipart 参数形式
@PartMap Map<String, RequestBody> map,
@Part List<MultipartBody.Part> parts
注：Map中可以用String也可以用RequestBody

```
@Multipart
@POST("api/Express/add")
Observable<BaseModel<Object>> expressAdd(@PartMap Map<String, RequestBody> map,
                                             @Part List<MultipartBody.Part> parts);
```

```
public interface UpLoadView extends BaseView {
    void onExpressAddSuccess(BaseModel<Object> o);
}
public class UpLoadPresenter extends BasePresenter<UpLoadView> {
    public UpLoadPresenter(UpLoadView baseView) {
        super(baseView);
    }

    public void expressAdd(String title, String content, List<MultipartBody.Part> parts) {
        HashMap<String, RequestBody> params = new HashMap<>();
        params.put("title", RetrofitUtil.convertToRequestBody(title));
        params.put("content", RetrofitUtil.convertToRequestBody(content));
        addDisposable(apiServer.expressAdd(params, parts), new BaseObserver(baseView) {
            @Override
            public void onSuccess(Object o) {
                baseView.onExpressAddSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }
}
@Override
    public void onClick(View v) {
        /**
         * 俩个参数  一个是图片集合路径   一个是和后台约定的Key，如果后台不需要，随便写都行
         */
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            strings.add("tupian.lujing");
        }
        mPresenter.expressAdd(
                "title",
                "content",
                RetrofitUtil.filesToMultipartBodyParts(RetrofitUtil.initImages(strings), "tupian.key"));
    }
```
**至此，请求内部模块已经写完，以下内容为如何请求。**

最后将所需请求代码封装到activity基类，这样在activity省略了很多重复代码，对代码的阅读性提升了很多
BaseActivity基类如下：
```

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.hjq.toast.ToastUtils;
import com.willkong.mvp_network.base.mvp.BaseModel;
import com.willkong.mvp_network.base.mvp.BasePresenter;
import com.willkong.mvp_network.base.mvp.BaseView;
import com.willkong.mvp_network.utils.KeyBoardUtils;
import com.willkong.mvp_network.utils.StatusBarUtil;
import com.willkong.mvp_network.view.LoadingDialog;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base
 * @Author: willkong
 * @CreateDate: 2019/7/25 18:20
 * @Description: activity基类
 */
public abstract class BaseActivity<P extends BasePresenter> extends AppCompatActivity implements BaseView {
    protected final String TAG = this.getClass().getSimpleName();
    public Context mContext;
    protected P mPresenter;

    protected abstract P createPresenter();

    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(getLayoutId());
        mPresenter = createPresenter();
        setStatusBar();

        this.initData();
    }

    /**
     * 获取布局ID
     *
     * @return
     */
    protected abstract int getLayoutId();

    /**
     * 数据初始化操作
     */
    protected abstract void initData();

    /**
     * 此处设置沉浸式地方
     */
    protected void setStatusBar() {
        StatusBarUtil.setTranslucentForImageViewInFragment(this, 0, null);
    }

    /**
     * 封装toast方法（自行定制实现）
     *
     * @param str
     */
    public void showToast(String str) {
        ToastUtils.show(str);
    }

    public void showLongToast(String str) {
        ToastUtils.show(str);
    }

    @Override
    public void showError(String msg) {
        showToast(msg);
    }

    /**
     * 返回所有状态  除去指定的值  可设置所有（根据需求）
     *
     * @param model
     */
    @Override
    public void onErrorCode(BaseModel model) {
        if (model.getErrcode() == 10000000) {
            //处理些后续逻辑   如果某个页面不想实现  子类重写这个方法  将super去掉  自定义方法
//            App.put();
//            startActivity(LoginActivity.class);
        }
    }

    @Override
    public void showLoading() {
        showLoadingDialog();
    }

    @Override
    public void hideLoading() {
        dissMissDialog();
    }

    public void showLoadingDialog() {
        showLoadingDialog("加载中...");
    }

    /**
     * 加载  黑框...
     */
    public void showLoadingDialog(String msg) {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(this);
        }
        loadingDialog.setMessage(msg);
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    /**
     * 消失  黑框...
     */
    public void dissMissDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.detachView();
        }
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
        if (mPresenter != null) {
            mPresenter.detachView();
        }
    }


    /**
     * [页面跳转]
     *
     * @param clz
     */
    public void startActivity(Class<?> clz) {
        startActivity(clz, null);
    }


    /**
     * [携带数据的页面跳转]
     *
     * @param clz
     * @param bundle
     */
    public void startActivity(Class<?> clz, Bundle bundle) {
        Intent intent = new Intent();
        intent.setClass(this, clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    /**
     * [含有Bundle通过Class打开编辑界面]
     *
     * @param cls
     * @param bundle
     * @param requestCode
     */
    public void startActivityForResult(Class<?> cls, Bundle bundle, int requestCode) {
        Intent intent = new Intent();
        intent.setClass(this, cls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivityForResult(intent, requestCode);
    }

    /**
     * 以下是关于软键盘的处理
     */

    /**
     * 清除editText的焦点
     *
     * @param v   焦点所在View
     * @param ids 输入框
     */
    public void clearViewFocus(View v, int... ids) {
        if (null != v && null != ids && ids.length > 0) {
            for (int id : ids) {
                if (v.getId() == id) {
                    v.clearFocus();
                    break;
                }
            }
        }
    }

    /**
     * 隐藏键盘
     *
     * @param v   焦点所在View
     * @param ids 输入框
     * @return true代表焦点在edit上
     */
    public boolean isFocusEditText(View v, int... ids) {
        if (v instanceof EditText) {
            EditText et = (EditText) v;
            for (int id : ids) {
                if (et.getId() == id) {
                    return true;
                }
            }
        }
        return false;
    }

    //是否触摸在指定view上面,对某个控件过滤
    public boolean isTouchView(View[] views, MotionEvent ev) {
        if (views == null || views.length == 0) {
            return false;
        }
        int[] location = new int[2];
        for (View view : views) {
            view.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            if (ev.getX() > x && ev.getX() < (x + view.getWidth())
                    && ev.getY() > y && ev.getY() < (y + view.getHeight())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (isTouchView(filterViewByIds(), ev)) {
                return super.dispatchTouchEvent(ev);
            }
            if (hideSoftByEditViewIds() == null || hideSoftByEditViewIds().length == 0) {
                return super.dispatchTouchEvent(ev);
            }
            View v = getCurrentFocus();
            if (isFocusEditText(v, hideSoftByEditViewIds())) {
                KeyBoardUtils.hideInputForce(this);
                clearViewFocus(v, hideSoftByEditViewIds());
            }
        }
        return super.dispatchTouchEvent(ev);
    }


    /**
     * 传入EditText的Id
     * 没有传入的EditText不做处理
     *
     * @return id 数组
     */
    public int[] hideSoftByEditViewIds() {
        return null;
    }

    /**
     * 传入要过滤的View
     * 过滤之后点击将不会有隐藏软键盘的操作
     *
     * @return id 数组
     */
    public View[] filterViewByIds() {
        return null;
    }


    /*实现案例===============================================================================================*/
    /*

    @Override
    public int[] hideSoftByEditViewIds() {
        int[] ids = {R.id.et_company_name, R.id.et_address};
        return ids;
    }

    @Override
    public View[] filterViewByIds() {
        View[] views = {mEtCompanyName, mEtAddress};
        return views;
    }

    */
}

```
BaseFragment基类如下：
```

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.willkong.mvp_network.base.mvp.BaseModel;
import com.willkong.mvp_network.base.mvp.BasePresenter;
import com.willkong.mvp_network.base.mvp.BaseView;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base
 * @Author: willkong
 * @CreateDate: 2019/7/25 18:30
 * @Description: ftagment 基类
 */

public abstract class BaseFragment<P extends BasePresenter> extends Fragment implements BaseView {
    public View view;

    public Context mContext;
    protected P mPresenter;

    protected abstract P createPresenter();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(getLayoutId(), container, false);

        mContext = getActivity();
        mPresenter = createPresenter();

        this.initToolbar(savedInstanceState);
        this.initData();

        return view;
    }

    /**
     * 获取布局ID
     *
     * @return
     */
    protected abstract int getLayoutId();

    /**
     * 处理顶部title
     *
     * @param savedInstanceState
     */
    protected abstract void initToolbar(Bundle savedInstanceState);


    /**
     * 数据初始化操作
     */
    protected abstract void initData();

    public void showToast(String str) {
    }

    public void showLongToast(String str) {
    }

    @Override
    public void showError(String msg) {
        showToast(msg);
    }

    @Override
    public void onErrorCode(BaseModel model) {
    }

    @Override
    public void showLoading() {
    }

    @Override
    public void hideLoading() {
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        this.view = null;
        if (mPresenter != null) {
            mPresenter.detachView();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /**
     * [页面跳转]
     *
     * @param clz
     */
    public void startActivity(Class<?> clz) {
        startActivity(clz, null);
    }


    /**
     * [携带数据的页面跳转]
     *
     * @param clz
     * @param bundle
     */
    public void startActivity(Class<?> clz, Bundle bundle) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    /**
     * [含有Bundle通过Class打开编辑界面]
     *
     * @param cls
     * @param bundle
     * @param requestCode
     */
    public void startActivityForResult(Class<?> cls, Bundle bundle,
                                       int requestCode) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), cls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivityForResult(intent, requestCode);
    }
}

```
KeyBoardUtils

```

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * <pre>
 *  author : wyz
 *  e_mail : xxx@xx
 *  time  : 2017/06/${DYA}
 *  desc :
 *  version: 1.0
 * </pre>
 */

public class KeyBoardUtils {

    /**
     * 打开键盘
     **/
    public static void openKeybord(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!imm.isActive()) {
            imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 2. * 关闭软键盘
     * 3. *
     * 4. * @param v
     * 5.
     */
    public static void closeKeybord(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
        }
    }


    /**
     * 设置键盘显示与隐藏
     *
     * @param context
     * @param view
     * @param visible
     */
    public static boolean setIMM(Context context, View view, boolean visible) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (visible) {
            return imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        } else {
            return imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        }
    }


    /**
     * des:隐藏软键盘,这种方式参数为activity
     * 但没有失去焦点
     *
     * @param activity
     */
    public static void hideInputForce(Activity activity) {
        if (activity == null || activity.getCurrentFocus() == null)
            return;

        ((InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(activity.getCurrentFocus()
                        .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * 打开软键盘
     * 魅族可能会有问题
     *
     * @param mEditText
     * @param mContext
     */
    public static void showInput(EditText mEditText, Context mContext) {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditText, InputMethodManager.RESULT_SHOWN);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

}

```
StatusBarUtil

```

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.willkong.mvp_network.R;


/**
 * Created by Jaeger on 16/2/14.
 * <p>
 * Email: chjie.jaeger@gmail.com
 * GitHub: https://github.com/laobie
 */
public class StatusBarUtil {

    public static final int DEFAULT_STATUS_BAR_ALPHA = 112;
    private static final int FAKE_STATUS_BAR_VIEW_ID = R.id.statusbarutil_fake_status_bar_view;
    private static final int FAKE_TRANSLUCENT_VIEW_ID = R.id.statusbarutil_translucent_view;
    private static final int TAG_KEY_HAVE_SET_OFFSET = -123;

    /**
     * 设置状态栏颜色
     *
     * @param activity 需要设置的 activity
     * @param color    状态栏颜色值
     */
    public static void setColor(Activity activity, @ColorInt int color) {
        setColor(activity, color, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * 设置状态栏颜色
     *
     * @param activity       需要设置的activity
     * @param color          状态栏颜色值
     * @param statusBarAlpha 状态栏透明度
     */

    public static void setColor(Activity activity, @ColorInt int color, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().setStatusBarColor(calculateStatusColor(color, statusBarAlpha));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            View fakeStatusBarView = decorView.findViewById(FAKE_STATUS_BAR_VIEW_ID);
            if (fakeStatusBarView != null) {
                if (fakeStatusBarView.getVisibility() == View.GONE) {
                    fakeStatusBarView.setVisibility(View.VISIBLE);
                }
                fakeStatusBarView.setBackgroundColor(calculateStatusColor(color, statusBarAlpha));
            } else {
                decorView.addView(createStatusBarView(activity, color, statusBarAlpha));
            }
            setRootView(activity);
        }
    }

    /**
     * 为滑动返回界面设置状态栏颜色
     *
     * @param activity 需要设置的activity
     * @param color    状态栏颜色值
     */
    public static void setColorForSwipeBack(Activity activity, int color) {
        setColorForSwipeBack(activity, color, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * 为滑动返回界面设置状态栏颜色
     *
     * @param activity       需要设置的activity
     * @param color          状态栏颜色值
     * @param statusBarAlpha 状态栏透明度
     */
    public static void setColorForSwipeBack(Activity activity, @ColorInt int color, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            ViewGroup contentView = ((ViewGroup) activity.findViewById(android.R.id.content));
            View rootView = contentView.getChildAt(0);
            int statusBarHeight = getStatusBarHeight(activity);
            if (rootView != null && rootView instanceof CoordinatorLayout) {
                final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) rootView;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    coordinatorLayout.setFitsSystemWindows(false);
                    contentView.setBackgroundColor(calculateStatusColor(color, statusBarAlpha));
                    boolean isNeedRequestLayout = contentView.getPaddingTop() < statusBarHeight;
                    if (isNeedRequestLayout) {
                        contentView.setPadding(0, statusBarHeight, 0, 0);
                        coordinatorLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                coordinatorLayout.requestLayout();
                            }
                        });
                    }
                } else {
                    coordinatorLayout.setStatusBarBackgroundColor(calculateStatusColor(color, statusBarAlpha));
                }
            } else {
                contentView.setPadding(0, statusBarHeight, 0, 0);
                contentView.setBackgroundColor(calculateStatusColor(color, statusBarAlpha));
            }
            setTransparentForWindow(activity);
        }
    }

    /**
     * 设置状态栏纯色 不加半透明效果
     *
     * @param activity 需要设置的 activity
     * @param color    状态栏颜色值
     */
    public static void setColorNoTranslucent(Activity activity, @ColorInt int color) {
        setColor(activity, color, 0);
    }

    /**
     * 设置状态栏颜色(5.0以下无半透明效果,不建议使用)
     *
     * @param activity 需要设置的 activity
     * @param color    状态栏颜色值
     */
    @Deprecated
    public static void setColorDiff(Activity activity, @ColorInt int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        transparentStatusBar(activity);
        ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        // 移除半透明矩形,以免叠加
        View fakeStatusBarView = contentView.findViewById(FAKE_STATUS_BAR_VIEW_ID);
        if (fakeStatusBarView != null) {
            if (fakeStatusBarView.getVisibility() == View.GONE) {
                fakeStatusBarView.setVisibility(View.VISIBLE);
            }
            fakeStatusBarView.setBackgroundColor(color);
        } else {
            contentView.addView(createStatusBarView(activity, color));
        }
        setRootView(activity);
    }

    /**
     * 使状态栏半透明
     * <p>
     * 适用于图片作为背景的界面,此时需要图片填充到状态栏
     *
     * @param activity 需要设置的activity
     */
    public static void setTranslucent(Activity activity) {
        setTranslucent(activity, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * 使状态栏半透明
     * <p>
     * 适用于图片作为背景的界面,此时需要图片填充到状态栏
     *
     * @param activity       需要设置的activity
     * @param statusBarAlpha 状态栏透明度
     */
    public static void setTranslucent(Activity activity, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        setTransparent(activity);
        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * 针对根布局是 CoordinatorLayout, 使状态栏半透明
     * <p>
     * 适用于图片作为背景的界面,此时需要图片填充到状态栏
     *
     * @param activity       需要设置的activity
     * @param statusBarAlpha 状态栏透明度
     */
    public static void setTranslucentForCoordinatorLayout(Activity activity, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        transparentStatusBar(activity);
        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * 设置状态栏全透明
     *
     * @param activity 需要设置的activity
     */
    public static void setTransparent(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        transparentStatusBar(activity);
        setRootView(activity);
    }

    /**
     * 使状态栏透明(5.0以上半透明效果,不建议使用)
     * <p>
     * 适用于图片作为背景的界面,此时需要图片填充到状态栏
     *
     * @param activity 需要设置的activity
     */
    @Deprecated
    public static void setTranslucentDiff(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 设置状态栏透明
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            setRootView(activity);
        }
    }

    /**
     * 为DrawerLayout 布局设置状态栏变色
     *
     * @param activity     需要设置的activity
     * @param drawerLayout DrawerLayout
     * @param color        状态栏颜色值
     */
    public static void setColorForDrawerLayout(Activity activity, DrawerLayout drawerLayout, @ColorInt int color) {
        setColorForDrawerLayout(activity, drawerLayout, color, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * 为DrawerLayout 布局设置状态栏颜色,纯色
     *
     * @param activity     需要设置的activity
     * @param drawerLayout DrawerLayout
     * @param color        状态栏颜色值
     */
    public static void setColorNoTranslucentForDrawerLayout(Activity activity, DrawerLayout drawerLayout, @ColorInt int color) {
        setColorForDrawerLayout(activity, drawerLayout, color, 0);
    }

    /**
     * 为DrawerLayout 布局设置状态栏变色
     *
     * @param activity       需要设置的activity
     * @param drawerLayout   DrawerLayout
     * @param color          状态栏颜色值
     * @param statusBarAlpha 状态栏透明度
     */
    public static void setColorForDrawerLayout(Activity activity, DrawerLayout drawerLayout, @ColorInt int color,
                                               int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        // 生成一个状态栏大小的矩形
        // 添加 statusBarView 到布局中
        ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
        View fakeStatusBarView = contentLayout.findViewById(FAKE_STATUS_BAR_VIEW_ID);
        if (fakeStatusBarView != null) {
            if (fakeStatusBarView.getVisibility() == View.GONE) {
                fakeStatusBarView.setVisibility(View.VISIBLE);
            }
            fakeStatusBarView.setBackgroundColor(color);
        } else {
            contentLayout.addView(createStatusBarView(activity, color), 0);
        }
        // 内容布局不是 LinearLayout 时,设置padding top
        if (!(contentLayout instanceof LinearLayout) && contentLayout.getChildAt(1) != null) {
            contentLayout.getChildAt(1)
                    .setPadding(contentLayout.getPaddingLeft(), getStatusBarHeight(activity) + contentLayout.getPaddingTop(),
                            contentLayout.getPaddingRight(), contentLayout.getPaddingBottom());
        }
        // 设置属性
        setDrawerLayoutProperty(drawerLayout, contentLayout);
        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * 设置 DrawerLayout 属性
     *
     * @param drawerLayout              DrawerLayout
     * @param drawerLayoutContentLayout DrawerLayout 的内容布局
     */
    private static void setDrawerLayoutProperty(DrawerLayout drawerLayout, ViewGroup drawerLayoutContentLayout) {
        ViewGroup drawer = (ViewGroup) drawerLayout.getChildAt(1);
        drawerLayout.setFitsSystemWindows(false);
        drawerLayoutContentLayout.setFitsSystemWindows(false);
        drawerLayoutContentLayout.setClipToPadding(true);
        drawer.setFitsSystemWindows(false);
    }

    /**
     * 为DrawerLayout 布局设置状态栏变色(5.0以下无半透明效果,不建议使用)
     *
     * @param activity     需要设置的activity
     * @param drawerLayout DrawerLayout
     * @param color        状态栏颜色值
     */
    @Deprecated
    public static void setColorForDrawerLayoutDiff(Activity activity, DrawerLayout drawerLayout, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 生成一个状态栏大小的矩形
            ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
            View fakeStatusBarView = contentLayout.findViewById(FAKE_STATUS_BAR_VIEW_ID);
            if (fakeStatusBarView != null) {
                if (fakeStatusBarView.getVisibility() == View.GONE) {
                    fakeStatusBarView.setVisibility(View.VISIBLE);
                }
                fakeStatusBarView.setBackgroundColor(calculateStatusColor(color, DEFAULT_STATUS_BAR_ALPHA));
            } else {
                // 添加 statusBarView 到布局中
                contentLayout.addView(createStatusBarView(activity, color), 0);
            }
            // 内容布局不是 LinearLayout 时,设置padding top
            if (!(contentLayout instanceof LinearLayout) && contentLayout.getChildAt(1) != null) {
                contentLayout.getChildAt(1).setPadding(0, getStatusBarHeight(activity), 0, 0);
            }
            // 设置属性
            setDrawerLayoutProperty(drawerLayout, contentLayout);
        }
    }

    /**
     * 为 DrawerLayout 布局设置状态栏透明
     *
     * @param activity     需要设置的activity
     * @param drawerLayout DrawerLayout
     */
    public static void setTranslucentForDrawerLayout(Activity activity, DrawerLayout drawerLayout) {
        setTranslucentForDrawerLayout(activity, drawerLayout, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * 为 DrawerLayout 布局设置状态栏透明
     *
     * @param activity     需要设置的activity
     * @param drawerLayout DrawerLayout
     */
    public static void setTranslucentForDrawerLayout(Activity activity, DrawerLayout drawerLayout, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        setTransparentForDrawerLayout(activity, drawerLayout);
        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * 为 DrawerLayout 布局设置状态栏透明
     *
     * @param activity     需要设置的activity
     * @param drawerLayout DrawerLayout
     */
    public static void setTransparentForDrawerLayout(Activity activity, DrawerLayout drawerLayout) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
        // 内容布局不是 LinearLayout 时,设置padding top
        if (!(contentLayout instanceof LinearLayout) && contentLayout.getChildAt(1) != null) {
            contentLayout.getChildAt(1).setPadding(0, getStatusBarHeight(activity), 0, 0);
        }

        // 设置属性
        setDrawerLayoutProperty(drawerLayout, contentLayout);
    }

    /**
     * 为 DrawerLayout 布局设置状态栏透明(5.0以上半透明效果,不建议使用)
     *
     * @param activity     需要设置的activity
     * @param drawerLayout DrawerLayout
     */
    @Deprecated
    public static void setTranslucentForDrawerLayoutDiff(Activity activity, DrawerLayout drawerLayout) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 设置状态栏透明
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 设置内容布局属性
            ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
            contentLayout.setFitsSystemWindows(true);
            contentLayout.setClipToPadding(true);
            // 设置抽屉布局属性
            ViewGroup vg = (ViewGroup) drawerLayout.getChildAt(1);
            vg.setFitsSystemWindows(false);
            // 设置 DrawerLayout 属性
            drawerLayout.setFitsSystemWindows(false);
        }
    }

    /**
     * 为头部是 ImageView 的界面设置状态栏全透明
     *
     * @param activity       需要设置的activity
     * @param needOffsetView 需要向下偏移的 View
     */
    public static void setTransparentForImageView(Activity activity, View needOffsetView) {
        setTranslucentForImageView(activity, 0, needOffsetView);
    }

    /**
     * 为头部是 ImageView 的界面设置状态栏透明(使用默认透明度)
     *
     * @param activity       需要设置的activity
     * @param needOffsetView 需要向下偏移的 View
     */
    public static void setTranslucentForImageView(Activity activity, View needOffsetView) {
        setTranslucentForImageView(activity, DEFAULT_STATUS_BAR_ALPHA, needOffsetView);
    }

    /**
     * 为头部是 ImageView 的界面设置状态栏透明
     *
     * @param activity       需要设置的activity
     * @param statusBarAlpha 状态栏透明度
     * @param needOffsetView 需要向下偏移的 View
     */
    public static void setTranslucentForImageView(Activity activity, int statusBarAlpha, View needOffsetView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        setTransparentForWindow(activity);
        addTranslucentView(activity, statusBarAlpha);
        if (needOffsetView != null) {
            Object haveSetOffset = needOffsetView.getTag(TAG_KEY_HAVE_SET_OFFSET);
            if (haveSetOffset != null && (Boolean) haveSetOffset) {
                return;
            }
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) needOffsetView.getLayoutParams();
            layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin + getStatusBarHeight(activity),
                    layoutParams.rightMargin, layoutParams.bottomMargin);
            needOffsetView.setTag(TAG_KEY_HAVE_SET_OFFSET, true);
        }
    }

    /**
     * 为 fragment 头部是 ImageView 的设置状态栏透明
     *
     * @param activity       fragment 对应的 activity
     * @param needOffsetView 需要向下偏移的 View
     */
    public static void setTranslucentForImageViewInFragment(Activity activity, View needOffsetView) {
        setTranslucentForImageViewInFragment(activity, DEFAULT_STATUS_BAR_ALPHA, needOffsetView);
    }

    /**
     * 为 fragment 头部是 ImageView 的设置状态栏透明
     *
     * @param activity       fragment 对应的 activity
     * @param needOffsetView 需要向下偏移的 View
     */
    public static void setTransparentForImageViewInFragment(Activity activity, View needOffsetView) {
        setTranslucentForImageViewInFragment(activity, 0, needOffsetView);
    }

    /**
     * 为 fragment 头部是 ImageView 的设置状态栏透明
     *
     * @param activity       fragment 对应的 activity
     * @param statusBarAlpha 状态栏透明度
     * @param needOffsetView 需要向下偏移的 View
     */
    public static void setTranslucentForImageViewInFragment(Activity activity, int statusBarAlpha, View needOffsetView) {
        setTranslucentForImageView(activity, statusBarAlpha, needOffsetView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            clearPreviousSetting(activity);
        }
    }

    /**
     * 隐藏伪状态栏 View
     *
     * @param activity 调用的 Activity
     */
    public static void hideFakeStatusBarView(Activity activity) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View fakeStatusBarView = decorView.findViewById(FAKE_STATUS_BAR_VIEW_ID);
        if (fakeStatusBarView != null) {
            fakeStatusBarView.setVisibility(View.GONE);
        }
        View fakeTranslucentView = decorView.findViewById(FAKE_TRANSLUCENT_VIEW_ID);
        if (fakeTranslucentView != null) {
            fakeTranslucentView.setVisibility(View.GONE);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void clearPreviousSetting(Activity activity) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View fakeStatusBarView = decorView.findViewById(FAKE_STATUS_BAR_VIEW_ID);
        if (fakeStatusBarView != null) {
            decorView.removeView(fakeStatusBarView);
            ViewGroup rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            rootView.setPadding(0, 0, 0, 0);
        }
    }

    /**
     * 添加半透明矩形条
     *
     * @param activity       需要设置的 activity
     * @param statusBarAlpha 透明值
     */
    private static void addTranslucentView(Activity activity, int statusBarAlpha) {
        ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        View fakeTranslucentView = contentView.findViewById(FAKE_TRANSLUCENT_VIEW_ID);
        if (fakeTranslucentView != null) {
            if (fakeTranslucentView.getVisibility() == View.GONE) {
                fakeTranslucentView.setVisibility(View.VISIBLE);
            }
            fakeTranslucentView.setBackgroundColor(Color.argb(statusBarAlpha, 0, 0, 0));
        } else {
            contentView.addView(createTranslucentStatusBarView(activity, statusBarAlpha));
        }
    }

    /**
     * 生成一个和状态栏大小相同的彩色矩形条
     *
     * @param activity 需要设置的 activity
     * @param color    状态栏颜色值
     * @return 状态栏矩形条
     */
    private static View createStatusBarView(Activity activity, @ColorInt int color) {
        return createStatusBarView(activity, color, 0);
    }

    /**
     * 生成一个和状态栏大小相同的半透明矩形条
     *
     * @param activity 需要设置的activity
     * @param color    状态栏颜色值
     * @param alpha    透明值
     * @return 状态栏矩形条
     */
    private static View createStatusBarView(Activity activity, @ColorInt int color, int alpha) {
        // 绘制一个和状态栏一样高的矩形
        View statusBarView = new View(activity);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(calculateStatusColor(color, alpha));
        statusBarView.setId(FAKE_STATUS_BAR_VIEW_ID);
        return statusBarView;
    }

    /**
     * 设置根布局参数
     */
    private static void setRootView(Activity activity) {
        ViewGroup parent = (ViewGroup) activity.findViewById(android.R.id.content);
        for (int i = 0, count = parent.getChildCount(); i < count; i++) {
            View childView = parent.getChildAt(i);
            if (childView instanceof ViewGroup) {
                childView.setFitsSystemWindows(true);
                ((ViewGroup) childView).setClipToPadding(true);
            }
        }
    }

    /**
     * 设置透明
     */
    public static void setTransparentForWindow(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            activity.getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow()
                    .setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 使状态栏透明
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void transparentStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 创建半透明矩形 View
     *
     * @param alpha 透明值
     * @return 半透明 View
     */
    private static View createTranslucentStatusBarView(Activity activity, int alpha) {
        // 绘制一个和状态栏一样高的矩形
        View statusBarView = new View(activity);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
        statusBarView.setId(FAKE_TRANSLUCENT_VIEW_ID);
        return statusBarView;
    }

    /**
     * 获取状态栏高度
     *
     * @param context context
     * @return 状态栏高度
     */
    private static int getStatusBarHeight(Context context) {
        // 获得状态栏高度
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return context.getResources().getDimensionPixelSize(resourceId);
    }

    /**
     * 计算状态栏颜色
     *
     * @param color color值
     * @param alpha alpha值
     * @return 最终的状态栏颜色
     */
    private static int calculateStatusColor(@ColorInt int color, int alpha) {
        if (alpha == 0) {
            return color;
        }
        float a = 1 - alpha / 255f;
        int red = color >> 16 & 0xff;
        int green = color >> 8 & 0xff;
        int blue = color & 0xff;
        red = (int) (red * a + 0.5);
        green = (int) (green * a + 0.5);
        blue = (int) (blue * a + 0.5);
        return 0xff << 24 | red << 16 | green << 8 | blue;
    }
}

```
ids.xml

```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <item type="id" name="statusbarutil_fake_status_bar_view" />
    <item type="id" name="statusbarutil_translucent_view" />
    <item name="tag_multistateview" type="id"/>

    <item name="id_stickynavlayout_topview" type="id"/>
    <item name="id_stickynavlayout_viewpager" type="id"/>
    <item name="id_stickynavlayout_indicator" type="id"/>
    <item name="id_stickynavlayout_innerscrollview" type="id"/>
</resources>
```
**以上内容为MVP+Retrofit2+okhttp3+Rxjava2全部封装，其间有些地方需根据自己项目内容所做修改，下边为大家演示下如何在对应activity请求数据**

##### 请求步骤使用示例：
**请求时有三个步骤，步骤如下**
**1.新建接口实体类，注意内容，抛去baseModel里边的内容，也就是抛去每个接口固定返回的字段，如code，message**
比如实体类如下

```
public class MainBean {
    /**
     * id : 11
     * act_logo : http://www.energy-link.com.cn/upload/admin/20180828/s_29a692567d0f0d84d515eb5cf5be98d0.jpg
     * play_time : 2018-06-10
     * name : 中国生物质能源产业联盟会员代表大会
     * province : 北京市
     * city : 西城区
     */
    private int id;
    private String act_logo;
    private String play_time;
    private String name;
    private String province;
    private String city;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getAct_logo() {
        return act_logo;
    }
    public void setAct_logo(String act_logo) {
        this.act_logo = act_logo;
    }

    public String getPlay_time() {
        return play_time;
    }
    public void setPlay_time(String play_time) {
        this.play_time = play_time;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getProvince() {
        return province;
    }
    public void setProvince(String province) {
        this.province = province;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
}
```
**2.新建对应的接口回调view**

```
import com.lp.mvp_network.base.mvp.BaseModel;
import com.lp.mvp_network.base.mvp.BaseView;
import java.util.List;

public interface MainView extends BaseView {
    void onMainSuccess(BaseModel<List<MainBean>> o);
}
```
**3.新建对应的请求Presenter**

```
import com.lp.mvp_network.base.mvp.BaseModel;
import com.lp.mvp_network.base.mvp.BaseObserver;
import com.lp.mvp_network.base.mvp.BasePresenter;

import java.util.List;

/**
 * File descripition:
 *
 * @date 2018/6/19
 */

public class MainPresenter extends BasePresenter<MainView> {
    public MainPresenter(MainView baseView) {
        super(baseView);
    }

    public void commentAdd() {
        addDisposable(apiServer.getMain("year"), new BaseObserver(baseView) {
            @Override
            public void onSuccess(Object o) {
                baseView.onMainSuccess((BaseModel<List<MainBean>>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }
    
    /**
     * 写法好多种  怎么顺手怎么来
     */
    public void getManApi() {
        addDisposable(apiServer.getMain("year"), new BaseObserver(baseView) {
            @Override
            public void onSuccess(BaseModel o) {
                baseView.onMainSuccess((BaseModel<List<MainBean>>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }

    /**
     * 写法好多种  怎么顺手怎么来
     */
    public void getMan2Api() {
        addDisposable(apiServer.getMain2("year"), new BaseObserver(baseView) {
            @Override
            public void onSuccess(BaseModel o) {
                baseView.onMainSuccess((BaseModel<List<MainBean>>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }

    /**
     * 写法好多种  怎么顺手怎么来
     */
    public void getMan3Api() {
        HashMap<String, String> params = new HashMap<>();
        params.put("time", "year");
        addDisposable(apiServer.getMain3(params), new BaseObserver(baseView) {

            @Override
            public void onSuccess(BaseModel o) {
                baseView.onMainSuccess((BaseModel<List<MainBean>>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }


    /*>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  图片上传  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/

    /**
     * 演示单图上传
     *
     * @param parts
     */
    public void upLoadImgApi(MultipartBody.Part parts) {
        addDisposable(apiServer.upLoadImg(parts), new BaseObserver(baseView) {

            @Override
            public void onSuccess(BaseModel o) {
                baseView.onUpLoadImgSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }


    /**
     * 演示多图上传
     *
     * @param parts
     */
    public void upLoadImgApi(List<MultipartBody.Part> parts) {
        addDisposable(apiServer.upHeadImg(parts), new BaseObserver(baseView) {
            @Override
            public void onSuccess(BaseModel o) {
                baseView.onUpLoadImgSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }

    /**
     * 演示 图片和字段一起上传
     *
     * @param title
     * @param content
     * @param parts
     */
    public void upLoadImgApi(String title, String content, List<MultipartBody.Part> parts) {
        HashMap<String, RequestBody> params = new HashMap<>();
        params.put("title", RetrofitUtil.convertToRequestBody(title));
        params.put("content", RetrofitUtil.convertToRequestBody(content));
        addDisposable(apiServer.expressAdd(params, parts), new BaseObserver(baseView) {
            @Override
            public void onSuccess(BaseModel o) {
                baseView.onUpLoadImgSuccess((BaseModel<Object>) o);
            }

            @Override
            public void onError(String msg) {
                if (baseView != null) {
                    baseView.showError(msg);
                }
            }
        });
    }
}
```
**4.在activity实现Presenter，比如mainActivity**

```
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.lp.mvp_network.R;
import com.lp.mvp_network.base.BaseActivity;
import com.lp.mvp_network.base.mvp.BaseModel;
import java.util.List;
public class MainActivity extends BaseActivity<MainPresenter> implements MainView, View.OnClickListener {
    private TextView tv_msg;
    private Button btn;
    @Override
    protected MainPresenter createPresenter() {
        return new MainPresenter(this);
    }
    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }
    @Override
    protected void initToolbar(Bundle savedInstanceState) {

    }
    @Override
    protected void initData() {
        tv_msg = findViewById(R.id.tv_msg);
        btn = findViewById(R.id.btn);
        btn.setOnClickListener(this);
    }
    @Override
    public void onMainSuccess(BaseModel<List<MainBean>> o) {
        //数据返回
        tv_msg.setText(o.getData().toString());
    }
    @Override
    public void onClick(View v) {
        //数据请求
        mPresenter.commentAdd();
    }
}
```
到这里，完整的框架封装完成。
**这里有人问dialog加载圈封装的不够好，这样每个接口都得显示加载圈，不想实现都不行**
答：BaseActivity和BaseFragment中都有这俩个方法

```
//显示加载进度框回调
    @Override
    public void showLoading() {
        showLoadingDialog();
    }
    //隐藏进度框回调
    @Override
    public void hideLoading() {
        closeLoadingDialog();
    }
```
如果说我本页面都不想显示Loading动画，那就在对应的Activity重写下父类的方法，比如

```
@Override
    public void showLoading() {
    //    super.showLoading();  //将super去掉  就不会显示Loading动画了
    }
```
如果我们需要显示就在对应的Fragment调用请求方法之后手动调一下父类的显示Loading方法，如下：

```
 mPresenter.collectApi("id");
 showLoadingDialog();
```
**假如接口返回1001，代表重写登录或者token失效，我想在对应activity拿到状态或者做统一操作**
答：可以在BaseActivity判断跳页面

```
//BaseActivity代码
 @Override
    public void onErrorCode(BaseModel model) {
       if (model.getErrcode() == 1001) {
            startLogin();
        }
    }

    private void startLogin() {
        startActivity(LoginActivity.class);
    }
```
如果想在对应Activity操作，那就在对应Activity重写此方法

```
//对应Activity代码
 @Override
    public void onErrorCode(BaseModel model) {
        //super.onErrorCode(model);
     if (model.getErrcode()==1001){
            //............................................
        }else if (model.getErrcode()==1002){
                  //............................................
        }
    }
```
**MVP+Retrofit2+okhttp3+Rxjava2网络请求封装完成。**
