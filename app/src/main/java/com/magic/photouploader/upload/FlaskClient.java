package com.magic.photouploader.upload;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;

/**
 * Created by lee on 2017/4/3.
 */

public interface FlaskClient {

    //上传图片
    @Multipart
    @POST("/upload")
    Call<UploadResult> uploadMultipleFiles(
            @PartMap Map<String, RequestBody> files);



}
