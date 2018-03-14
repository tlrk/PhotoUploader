package com.magic.photouploader;

import android.app.Activity;
import android.os.Bundle;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.lidong.photopicker.PhotoPickerActivity;
import com.lidong.photopicker.PhotoPreviewActivity;
import com.lidong.photopicker.SelectModel;
import com.lidong.photopicker.intent.PhotoPickerIntent;
import com.lidong.photopicker.intent.PhotoPreviewIntent;

import org.json.JSONArray;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE = 732;
    private static final int REQUEST_CAMERA_CODE = 10;
    private static final int REQUEST_PREVIEW_CODE = 20;
    private static final int MAX_PHOTO_SIZE = 9;

    private ArrayList<String> imagePaths = new ArrayList<>();
    private GridView gridView;
    private GridAdapter gridAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGridView();
//        Button bt = (Button) findViewById(R.id.from_album);
//        bt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // start multiple photos selector
//                Intent intent = new Intent(MainActivity.this, ImagesSelectorActivity.class);
//                // max number of images to be selected
//                intent.putExtra(SelectorSettings.SELECTOR_MAX_IMAGE_NUMBER, 9);
//                // min size of image which will be shown; to filter tiny images (mainly icons)
//                intent.putExtra(SelectorSettings.SELECTOR_MIN_IMAGE_SIZE, 100000);
//                // show camera or not
//                intent.putExtra(SelectorSettings.SELECTOR_SHOW_CAMERA, true);
//                // pass current selected images as the initial value
//                intent.putStringArrayListExtra(SelectorSettings.SELECTOR_INITIAL_SELECTED_LIST, imagePaths);
//                // start the selector
//                startActivityForResult(intent, REQUEST_CODE);
//            }
//        });

    }

    private void initGridView() {
        gridView = (GridView) findViewById(R.id.gridView);
        int cols = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().densityDpi;
        cols = cols < 3 ? 3 : cols;
        gridView.setNumColumns(cols);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String imgs = (String) parent.getItemAtPosition(position);
                if ("000000".equals(imgs) ){
                    PhotoPickerIntent intent = new PhotoPickerIntent(MainActivity.this);
                    intent.setSelectModel(SelectModel.MULTI);
                    intent.setShowCarema(true); // 是否显示拍照
                    intent.setMaxTotal(MAX_PHOTO_SIZE); // 最多选择照片数量，默认为6
                    intent.setSelectedPaths(imagePaths); // 已选中的照片地址， 用于回显选中状态
                    startActivityForResult(intent, REQUEST_CAMERA_CODE);
                }else{
                    PhotoPreviewIntent intent = new PhotoPreviewIntent(MainActivity.this);
                    intent.setCurrentItem(position);
                    intent.setPhotoPaths(imagePaths);
                    startActivityForResult(intent, REQUEST_PREVIEW_CODE);
                }
            }
        });
        imagePaths.add("000000");
        gridAdapter = new GridAdapter(imagePaths);
        gridView.setAdapter(gridAdapter);
    }

    private void loadAdapter(ArrayList<String> paths){
        if (imagePaths !=null&& imagePaths.size()>0){
            imagePaths.clear();
        }
        if (paths.contains("000000")){
            paths.remove("000000");
        }
        if (paths.size() < MAX_PHOTO_SIZE) {
            paths.add("000000");
        }
        imagePaths.addAll(paths);
        gridAdapter  = new GridAdapter(imagePaths);
        gridView.setAdapter(gridAdapter);
        try{
            JSONArray obj = new JSONArray(imagePaths);
            Log.e("--", obj.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // get selected images from selector
        super.onActivityResult(requestCode, resultCode, data);
            if(resultCode == RESULT_OK) {

                switch (requestCode) {
                    // 选择照片
                    case REQUEST_CAMERA_CODE:
                        ArrayList<String> list = data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT);
//                        Log.d(TAG, "list: " + "list = [" + list.size());
                        loadAdapter(list);
                        break;
                    // 预览
                    case REQUEST_PREVIEW_CODE:
                        ArrayList<String> ListExtra = data.getStringArrayListExtra(PhotoPreviewActivity.EXTRA_RESULT);
//                        Log.d(TAG, "ListExtra: " + "ListExtra = [" + ListExtra.size());
                        loadAdapter(ListExtra);
                        break;
                }
            }

//        if(requestCode == REQUEST_CODE) {
//            if(resultCode == RESULT_OK) {
//                imagePaths = data.getStringArrayListExtra(SelectorSettings.SELECTOR_RESULTS);
//                assert imagePaths != null;
//
//                // show results in textview
//                StringBuilder sb = new StringBuilder();
//                sb.append(String.format("Totally %d images selected:", imagePaths.size())).append("\n");
//                for(String result : imagePaths) {
//                    sb.append(result).append("\n");
//                }
//            }
//        }
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
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            final String path=listUrls.get(position);
            if (path.equals("000000")){
                holder.image.setImageResource(R.drawable.max_quick_bottom_icon_add_normal);
                holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }else {
                Glide.with(MainActivity.this)
                        .load(path)
                        .placeholder(R.mipmap.default_error)
                        .error(R.mipmap.default_error)
                        .centerCrop()
                        .crossFade()
                        .into(holder.image);
            }
            return convertView;
        }
        class ViewHolder {
            ImageView image;
        }
    }

}