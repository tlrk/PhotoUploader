package com.magic.photouploader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
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

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

import static com.magic.photouploader.R.id.progress;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CAMERA_CODE = 10;
    private static final int REQUEST_PREVIEW_CODE = 20;
    private static final int MAX_PHOTO_SIZE = 9;
    private static final int UPDATE_UI = 90;
    private static final int UPDATE_CHILD = 91;
    private static final String IMAGE_ADD_TAG = "000000";
    private static final String TAG = "MainActivity";
    private static final int UPLOAD_SUCCESS = 1;
    private static final int UPLOAD_UPLOADING = 2;
    private static final int UPLOAD_FAILED = 3;
    private int count = 0;

    private ArrayList<Request> mRequests = new ArrayList<>();
    private RecyclerView gridView;
    private ImageAdapter gridAdapter;
    private volatile boolean isUploading = false;

    Button button5;
    Button button15;
    Button button50;
    Button clear;

    ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGridView();
        mProgressBar = (ProgressBar) findViewById(progress);
        button5 = (Button) findViewById(R.id.button5);
        button15 = (Button) findViewById(R.id.button15);
        button50 = (Button) findViewById(R.id.button50);
        button5.setOnClickListener(this);
        button15.setOnClickListener(this);
        button50.setOnClickListener(this);

        clear = (Button) findViewById(R.id.clear);
        clear.setOnClickListener(this);
        FilesManager.getInstance().loadData(this.getApplicationContext());
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_UI) {
                updateView();
            } else if (msg.what == UPDATE_CHILD) {
                int index = msg.arg1;
                int status = msg.arg2;
                String showMsg = (String) msg.obj;
                mRequests.get(index).uploadMsg = showMsg;
                mRequests.get(index).uploadStatus = status;
                gridAdapter.notifyItemChanged(index);
            }
        }
    };

    private void initGridView() {
        gridView = (RecyclerView) findViewById(R.id.gridView);
        gridView.setLayoutManager(new GridLayoutManager(getBaseContext(), 3) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });

        Request request = new Request();
        request.filePath = IMAGE_ADD_TAG;
        mRequests.add(request);

        gridAdapter = new ImageAdapter(mRequests);
        gridView.setAdapter(gridAdapter);
    }

    private void loadAdapter(ArrayList<String> paths){
        mRequests.clear();
        if (paths.contains(IMAGE_ADD_TAG)){
            paths.remove(IMAGE_ADD_TAG);
        }
        if (paths.size() < MAX_PHOTO_SIZE) {
            paths.add(IMAGE_ADD_TAG);
        }

        for (int i = 0; i < paths.size(); i++) {
            Request request = new Request();
            request.filePath = paths.get(i);
            request.alreadyUploaded = FilesManager.getInstance().contains(request.filePath);
            request.uploadStatus = UPLOAD_UPLOADING;
            request.uploadMsg = String.valueOf(i + 1);
            mRequests.add(request);
        }
        gridAdapter  = new ImageAdapter(mRequests);
        gridView.setAdapter(gridAdapter);
    }

    private boolean needShowAlert() {
        if (mRequests.size() > 0) {
            for (Request request : mRequests) {
                if (request.alreadyUploaded) {
                    return true;
                }
            }
        }
        return false;
    }

    private void upload(final int weight) {
        if (mRequests.size() == 0 || (mRequests.size() == 1 && containsAdd())) {
            toast(getText(R.string.upload_hint));
        } else {
            if (needShowAlert()) {
                toast(getText(R.string.clear_first));
                return;
            }

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

    private boolean containsAdd() {
        if (mRequests.size() > 0) {
            for (int i = 0; i < mRequests.size(); i++) {
                if (IMAGE_ADD_TAG.equalsIgnoreCase(mRequests.get(i).filePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void uploadFiles(int weight) {
        isUploading = true;
        mHandler.sendEmptyMessage(UPDATE_UI);
        if(mRequests.size() == 0) {
            toast(getText(R.string.upload_hint));
            return;
        }

        final int size = containsAdd() ? mRequests.size() - 1 : mRequests.size();

        for (int index = 0; index < size; index++) {
            if (IMAGE_ADD_TAG.equalsIgnoreCase(mRequests.get(index).filePath)) {
                continue;
            } else {
                String filePath = mRequests.get(index).filePath;
                Uri uri = Uri.parse(filePath);
                File file = new File(uri.getPath());
                String mimeType= URLConnection.guessContentTypeFromName(file.getName());
                RequestBody surveyBody = RequestBody.create(MediaType.parse(mimeType),
                        file);
                MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), surveyBody);
                final FlaskClient service = ServiceGenerator.createService(FlaskClient.class);
                Call<UploadResult> call = service.uploadMultipleFiles(weight, index + 1, part);
                Message message = Message.obtain();
                message.what = UPDATE_CHILD;
                message.arg1 = index;
                message.arg2 = UPLOAD_UPLOADING;
                message.obj = getText(R.string.uploading);
                mHandler.sendMessage(message);

                String showMsg;
                int uploadStatus= UPLOAD_FAILED;
                try {
                    Response<UploadResult> response = call.execute();
                    Log.i("testDemo", response.body().msg + " index = " + index + 1);
                    if (response.isSuccessful()) {
                        if ("1".equalsIgnoreCase(response.body().msg)) {
                             showMsg = (String) getText(R.string.upload_success);
                            uploadStatus = UPLOAD_SUCCESS;
                            mRequests.get(index).alreadyUploaded = true;
                            FilesManager.getInstance().addUploadedFilePath(mRequests.get(index).filePath);
                        } else {
                            if (response.body().msg.contains("重复")) {
                                mRequests.get(index).alreadyUploaded = true;
                                FilesManager.getInstance().addUploadedFilePath(mRequests.get(index).filePath);
                            }
                            showMsg = response.body().msg;
                        }
                    } else {
                        showMsg = (String) getText(R.string.upload_failed);
                    }
                } catch (IOException e) {
                    showMsg = "网络错误";
                }

                Message message1 = Message.obtain();
                message1.what = UPDATE_CHILD;
                message1.arg1 = index;
                message1.arg2 = uploadStatus;
                message1.obj = showMsg;
                mHandler.sendMessageAtFrontOfQueue(message1);

                count ++;
                stopIfNecessary(size);
            }
        }
    }


    private void stopIfNecessary(int size) {
        if (count == size) {
            count = 0;
            isUploading = false;
            mHandler.sendEmptyMessageDelayed(UPDATE_UI, 100);
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
            case R.id.clear:
                clear();
                break;
        }
    }

    private void clear() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("清除")
                .setMessage("请选择清除所有图片还是清除已经上传图片").
                setNeutralButton("清除所有", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mRequests != null && mRequests.size() > 0) {
                    Iterator<Request> it = mRequests.iterator();
                    while(it.hasNext()){
                        Request request = it.next();
                        if(!TextUtils.equals(request.filePath, IMAGE_ADD_TAG )) {
                            it.remove();
                        }
                    }
                    gridAdapter.notifyDataSetChanged();
                    mHandler.sendEmptyMessage(UPDATE_UI);
                }
            }
        }).setNegativeButton("清除已上传", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (mRequests != null && mRequests.size() > 0) {
                    Iterator<Request> it = mRequests.iterator();
                    while(it.hasNext()){
                        Request request = it.next();
                        if(!TextUtils.equals(request.filePath, IMAGE_ADD_TAG ) && request.alreadyUploaded) {
                            it.remove();
                        }
                    }
                    if (mRequests.size() < MAX_PHOTO_SIZE && !containsAdd()) {
                        Request request = new Request();
                        request.filePath = IMAGE_ADD_TAG;
                        mRequests.add(request);
                    }
                    gridAdapter.notifyDataSetChanged();
                    mHandler.sendEmptyMessage(UPDATE_UI);
                }
            }
        }).setPositiveButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).create().show();
    }

    private void toast(CharSequence msg) {
        Toast toast = Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.SiteViewHolder> {

        private List<Request> mList;

        public ImageAdapter(List<Request> data) {
            this.mList = data;
            if(mList.size() == MAX_PHOTO_SIZE + 1){
                mList.remove(mList.size()-1);
            }
        }
        @Override
        public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SiteViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image, parent, false));
        }

        private ArrayList<String> getImagePaths () {
            ArrayList<String> imagePaths = new ArrayList<>();
            if (mList != null) {
                for (int i = 0; i < mList.size(); i++) {
                    imagePaths.add(mList.get(i).filePath);
                }
            }
            return imagePaths;
        }

        @Override
        public void onBindViewHolder(SiteViewHolder holder, final int position) {
            final Request request = mList.get(position);
            if (request.filePath.equals(IMAGE_ADD_TAG)){
                holder.image.setImageResource(R.drawable.ic_camera_alt_black_24dp);
                holder.image.setPadding(40, 40, 40, 40);
                holder.mTextView.setText(getText(R.string.add_text));
                holder.mTextView.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.black));
            }else {
                holder.image.setPadding(0, 0, 0, 0);
                if (request.alreadyUploaded) {
                    holder.mTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                    holder.mTextView.setText(getString(R.string.already_uploaded));
                } else {
                    if (request.uploadStatus == UPLOAD_UPLOADING) {
                        holder.mTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
                    } else {
                        holder.mTextView.setTextColor(ContextCompat.getColor(getApplicationContext(),
                                request.uploadStatus == UPLOAD_SUCCESS
                                        ? R.color.green : R.color.colorAccent));
                    }
                    holder.mTextView.setText(request.uploadMsg);
                }
                Glide.with(MainActivity.this)
                        .load(request.filePath)
                        .placeholder(R.mipmap.default_error)
                        .error(R.mipmap.default_error)
                        .centerCrop()
                        .crossFade()
                        .into(holder.image);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isUploading) {
                        return;
                    }
                    ArrayList<String> imagePaths = getImagePaths();
                    if (IMAGE_ADD_TAG.equals(request.filePath) ){
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
        }

        /**
         * Returns the total number of items in the data set held by the adapter.
         *
         * @return The total number of items in this adapter.
         */
        @Override
        public int getItemCount() {
            return mList.size();
        }

          class SiteViewHolder extends RecyclerView.ViewHolder {
              ImageView image;
              TextView mTextView;

            public SiteViewHolder(View itemView) {
                super(itemView);
                image = (ImageView) itemView.findViewById(R.id.imageView);
                mTextView = (TextView) itemView.findViewById(R.id.text);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FilesManager.getInstance().saveData(this.getApplicationContext());
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }
}