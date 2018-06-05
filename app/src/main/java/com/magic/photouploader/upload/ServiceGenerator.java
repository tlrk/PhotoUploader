package com.magic.photouploader.upload;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by lee on 2017/3/7.
 */

public class ServiceGenerator {

//    public static final String API_BASE_URL = "http://192.168.1.104:5000/";
    public static final String API_BASE_URL = "http://jessicababy.cn:5555/";
    // set your desired log level
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create());

    public static <S> S createService(Class<S> serviceClass) {
        Retrofit retrofit = builder.client(httpClient.build()).build();
        return retrofit.create(serviceClass);
    }
}