package com.willkong.mvp_network.base.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * @ProjectName: MVP-Retrofit2-okhttp3-Rxjava2
 * @Package: com.willkong.mvp_network.base.gson
 * @Author: willkong
 * @CreateDate: 2019/7/18 12:59
 * @Description: java类作用描述
 */

public class StringNullAdapter extends TypeAdapter<String> {
    @Override
    public String read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return "";//原先是返回Null，这里改为返回空字符串
        }

        String jsonStr = reader.nextString();
        if (jsonStr.equals("null")) {
            return "";
        } else {
            return jsonStr;
        }
    }

    @Override
    public void write(JsonWriter writer, String value) throws IOException {
        if (value == null) {
            writer.nullValue();
            return;
        }
        writer.value(value);
    }
}

