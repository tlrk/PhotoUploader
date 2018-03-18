package com.magic.photouploader;

import android.app.Activity;
import android.os.Bundle;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.lidong.photopicker.PhotoPickerActivity;
import com.lidong.photopicker.PhotoPreviewActivity;
import com.lidong.photopicker.SelectModel;
import com.lidong.photopicker.intent.PhotoPickerIntent;
import com.lidong.photopicker.intent.PhotoPreviewIntent;
import com.magic.photouploader.upload.FlaskClient;
import com.magic.photouploader.upload.Request;
import com.magic.photouploader.upload.ServiceGenerator;
import com.magic.photouploader.upload.UploadResult;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.magic.photouploader.R.id.progress;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CAMERA_CODE = 10;
    private static final int REQUEST_PREVIEW_CODE = 20;
    private static final int MAX_PHOTO_SIZE = 9;
    private static final int UPDATE_UI = 90;
    private static final String IMAGE_ADD_TAG = "000000";
    private static final String TAG = "MainActivity";
    private int count = 0;
    private EditText mLogView;

    private ArrayList<Request> mRequests = new ArrayList<>();
    private ArrayList<String> imagePaths = new ArrayList<>();
    private GridView gridView;
    private GridAdapter gridAdapter;
    private volatile boolean isUploading = false;

    Button button5;
    Button button15;
    Button button50;

    ProgressBar mProgressBar;
    StringBuffer logBuf = new StringBuffer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGridView();
        mProgressBar = (ProgressBar) findViewById(progress);
        button5 = (Button) findViewById(R.id.button5);
        button15 = (Button) findViewById(R.id.button15);
        button50 = (Button) findViewById(R.id.button50);
        mLogView = (EditText) findViewById(R.id.log);
        button5.setOnClickListener(this);
        button15.setOnClickListener(this);
        button50.setOnClickListener(this);
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_UI) {
                updateView();
            }
        }
    };

    private void initGridView() {
        gridView = (GridView) findViewById(R.id.gridView);
        int cols = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().densityDpi;
        cols = cols < 3 ? 3 : cols;
        gridView.setNumColumns(cols);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String imgs = (String) parent.getItemAtPosition(position);
                if (IMAGE_ADD_TAG.equals(imgs) ){
                    PhotoPickerIntent intent = new PhotoPickerIntent(MainActivity.this);
                    intent.setSelectModel(SelectModel.MULTI);
                    intent.setShowCarema(true); // 是否显示拍照
                    intent.setMaxTotal(MAX_PHOTO_SIZE); // 最多选择照片数量，默认为6
                    intent.setSelectedPaths(imagePaths); // 已选中的照片地址， 用于回显选中状态
                    startActivityForResult(intent, REQUEST_CAMERA_CODE);
                }else{
                    imagePaths.remove(IMAGE_ADD_TAG);
                    PhotoPreviewIntent intent = new PhotoPreviewIntent(MainActivity.this);
                    intent.setCurrentItem(position);
                    intent.setPhotoPaths(imagePaths);
                    startActivityForResult(intent, REQUEST_PREVIEW_CODE);
                }
            }
        });
        imagePaths.add(IMAGE_ADD_TAG);
        gridAdapter = new GridAdapter(imagePaths);
        gridView.setAdapter(gridAdapter);
    }

    private void loadAdapter(ArrayList<String> paths){
        if (imagePaths !=null&& imagePaths.size()>0){
            imagePaths.clear();
        }
        if (paths.contains(IMAGE_ADD_TAG)){
            paths.remove(IMAGE_ADD_TAG);
        }
        if (paths.size() < MAX_PHOTO_SIZE) {
            paths.add(IMAGE_ADD_TAG);
        }
        imagePaths.addAll(paths);
        gridAdapter  = new GridAdapter(imagePaths);
        gridView.setAdapter(gridAdapter);
        try{
            JSONArray obj = new JSONArray(imagePaths);
            Log.e(TAG, obj.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void upload(final int weight) {
        if (imagePaths.size() == 0 || (imagePaths.size() == 1 && imagePaths.contains(IMAGE_ADD_TAG))) {
            Toast.makeText(MainActivity.this, getText(R.string.upload_hint), Toast.LENGTH_SHORT).show();
        } else {
            mLogView.setText(getText(R.string.uploading));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    uploadFiles(weight);
                }
            }).start();
        }
    }

    private void updateView() {
        mProgressBar.setVisibility(isUploading? View.VISIBLE : View.INVISIBLE);
        button5.setEnabled(!isUploading);
        button15.setEnabled(!isUploading);
        button50.setEnabled(!isUploading);
        gridView.setEnabled(!isUploading);
    }

    public void uploadFiles(int weight) {
        isUploading = true;
        mHandler.sendEmptyMessage(UPDATE_UI);
        if(imagePaths.size() == 0) {
            Toast.makeText(MainActivity.this, getText(R.string.upload_hint), Toast.LENGTH_SHORT).show();
            return;
        }

        final int size = imagePaths.contains(IMAGE_ADD_TAG) ? imagePaths.size() - 1 : imagePaths.size();

        for (int index = 0; index < size; index++) {
            if (IMAGE_ADD_TAG.equalsIgnoreCase(imagePaths.get(index))) {
                continue;
            } else {
                File file = new File(imagePaths.get(index));
                RequestBody surveyBody = RequestBody.create(MediaType.parse(getMimeType(imagePaths.get(index))),
                        file);
                MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), surveyBody);
                final FlaskClient service = ServiceGenerator.createService(FlaskClient.class);
                Call<UploadResult> call = service.uploadMultipleFiles(weight, index + 1, part);

                call.enqueue(new Callback<UploadResult>() {
                    @Override
                    public void onResponse(Call<UploadResult> call, Response<UploadResult> response) {
                        count ++;
                        stopIfNecessary(size);
                        if (response.isSuccessful() && "1".equalsIgnoreCase(response.body().msg)) {
                            Toast.makeText(MainActivity.this, getText(R.string.upload_success), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, getText(R.string.upload_failed) + " : " +
                                    response.body().msg, Toast.LENGTH_SHORT).show();
                            logBuf.append(response.body().msg + "\n");
                        }
                    }

                    @Override
                    public void onFailure(Call<UploadResult> call, Throwable t) {
                        count ++;
                        stopIfNecessary(size);
                        logBuf.append(t.getMessage() + "\n");
                        Toast.makeText(MainActivity.this, getText(R.string.upload_failed) + " " +
                                t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }
    }


    private void stopIfNecessary(int size) {
        if (count == size) {
            mLogView.setText(logBuf.toString());
            logBuf.delete(0, logBuf.length() - 1);
            count = 0;
            isUploading = false;
            mHandler.sendEmptyMessage(UPDATE_UI);
        }
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // get selected images from selector
        super.onActivityResult(requestCode, resultCode, data);
            if(resultCode == RESULT_OK) {

                switch (requestCode) {
                    case REQUEST_CAMERA_CODE:
                        ArrayList<String> list = data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT);
                        loadAdapter(list);
                        break;
                    case REQUEST_PREVIEW_CODE:
                        ArrayList<String> ListExtra = data.getStringArrayListExtra(PhotoPreviewActivity.EXTRA_RESULT);
                        loadAdapter(ListExtra);
                        break;
                }
            }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button5:
                upload(5);
                break;
            case R.id.button15:
                upload(15);
                break;
            case R.id.button50:
                upload(50);
                break;
        }
    }

    private class GridAdapter extends BaseAdapter {
        private ArrayList<String> listUrls;
        private LayoutInflater inflater;
        public GridAdapter(ArrayList<String> listUrls) {
            this.listUrls = listUrls;
            if(listUrls.size() == MAX_PHOTO_SIZE + 1){
                listUrls.remove(listUrls.size()-1);
            }
            inflater = LayoutInflater.from(MainActivity.this);
        }

        public int getCount(){
            return  listUrls.size();
        }
        @Override
        public String getItem(int position) {
            return listUrls.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.item_image, parent,false);
                holder.image = (ImageView) convertView.findViewById(R.id.imageView);
                holder.mTextView = (TextView) convertView.findViewById(R.id.text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            final String path=listUrls.get(position);
            if (path.equals(IMAGE_ADD_TAG)){
                holder.image.setImageResource(R.drawable.ic_camera_alt_black_24dp);
                holder.image.setPadding(40, 40, 40, 40);
                holder.mTextView.setText(getText(R.string.add_text));
            }else {
                Glide.with(MainActivity.this)
                        .load(path)
                        .placeholder(R.mipmap.default_error)
                        .error(R.mipmap.default_error)
                        .centerCrop()
                        .crossFade()
                        .into(holder.image);
                holder.mTextView.setText(String.valueOf(position + 1));
            }
            return convertView;
        }
        class ViewHolder {
            ImageView image;
            TextView mTextView;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }
}