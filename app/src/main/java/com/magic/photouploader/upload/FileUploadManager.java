package com.magic.photouploader.upload;

/**
 * Created by tlrkboy on 13/03/2018.
 */


        import android.util.Log;

        import java.io.File;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.Map;

        import okhttp3.MediaType;
        import okhttp3.RequestBody;
        import retrofit2.Call;
        import retrofit2.Callback;
        import retrofit2.Response;
        import retrofit2.Retrofit;
        import retrofit2.converter.gson.GsonConverterFactory;
        import retrofit2.http.Multipart;
        import retrofit2.http.POST;
        import retrofit2.http.Part;
        import retrofit2.http.PartMap;


/**
 * Created by lidong on 2016/1/28.
 */
public class FileUploadManager {

    private static final String ENDPOINT = "http://192.168.1.21:8080";
    private static String TAG = FileUploadManager.class.getSimpleName();

    public interface FileUploadService {
        /**
         * 上传一张图片
         * @param description
         * @param imgs
         * @return
         */
        @Multipart
        @POST("/upload")
        Call<String> uploadImage(@Part("fileName") String description,
                                 @Part("file\"; filename=\"image.png\"") RequestBody imgs);


        /**
         * 上传6张图片
         * @param description
         * @param imgs1
         * @param imgs2
         * @param imgs3
         * @param imgs4
         * @param imgs5
         * @param imgs6
         * @return
         */
        @Multipart
        @POST("/upload")
        Call<String> uploadImage(@Part("description") String description,
                                 @Part("file\"; filename=\"image.png\"") RequestBody imgs1,
                                 @Part("file\"; filename=\"image.png\"") RequestBody imgs2,
                                 @Part("file\"; filename=\"image.png\"") RequestBody imgs3,
                                 @Part("file\"; filename=\"image.png\"") RequestBody imgs4,
                                 @Part("file\"; filename=\"image.png\"") RequestBody imgs5,
                                 @Part("file\"; filename=\"image.png\"") RequestBody imgs6);

        /**
         * 简便写法
         * @param description
         * @param imgs1
         * @return
         */
        @Multipart
        @POST("/upload")
        Call<String> uploadImage(@Part("description") String description,@PartMap
                Map<String, RequestBody> imgs1);
    }

    private static final Retrofit sRetrofit = new Retrofit .Builder()
            .baseUrl(ENDPOINT)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final FileUploadService apiManager = sRetrofit.create(FileUploadService.class);


    /**
     * 发说说
     * @param paths
     * @param desp
     */
    public static void upload(ArrayList<String> paths,String desp){
        RequestBody[] requestBody= new RequestBody[6];
        if (paths.size()>0) {
            for (int i=0;i<paths.size();i++) {
                requestBody[i] =
                        RequestBody.create(MediaType.parse("multipart/form-data"), new File(paths.get(i)));
            }
        }
        Call<String> call = apiManager.uploadImage( desp,requestBody[0],requestBody[1],requestBody[2],requestBody[3],requestBody[4],requestBody[5]);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d(TAG, "onResponse() called with: " + "call = [" + call + "], response = [" + response + "]");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d(TAG, "onFailure() called with: " + "call = [" + call + "], t = [" + t + "]");
            }
        });

    }

    /**
     *
     * @param paths
     * @param desp
     */
    public static void uploadMany(ArrayList<String> paths,String desp){
        Map<String,RequestBody> photos = new HashMap<>();
        if (paths.size()>0) {
            for (int i=0;i<paths.size();i++) {
                photos.put("photos"+i+"\"; filename=\"icon.png",  RequestBody.create(MediaType.parse("multipart/form-data"), new File(paths.get(i))));
            }
        }
        Call<String> stringCall = apiManager.uploadImage(desp, photos);
        stringCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d(TAG, "onResponse() called with: " + "call = [" + call + "], response = [" + response + "]");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d(TAG, "onFailure() called with: " + "call = [" + call + "], t = [" + t + "]");
            }
        });
    }
}
