package com.magic.photouploader;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by tlrkboy on 16/05/2018.
 */

public class FilesManager {

    private static FilesManager mInstance = new FilesManager();

    Set<String> mUploadedFilePaths = new HashSet<>();
    private static final String SAVE_NAME = "uploadedFiles";

    private FilesManager() {

    }

    public static FilesManager getInstance() {
        return mInstance;
    }

    public void loadData(Context context) {
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(SAVE_NAME, 0);
            String data = sp.getString("data", "[]");
            if (!TextUtils.isEmpty(data)) {
                try {
                    JSONArray jsonArray = new JSONArray(data);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        mUploadedFilePaths.add(jsonArray.getString(i));
                    }
                    Log.d(this.getClass().getSimpleName(), "load file success size = " + mUploadedFilePaths.size());
                } catch (JSONException e) {
                    Log.d(this.getClass().getSimpleName(), "load file failed");
                }
            }
        }
    }

    public void addUploadedFilePath(String filePath) {
        Log.d(getClass().getSimpleName(), "add file path = " + filePath);
        mUploadedFilePaths.add(filePath);
    }

    public boolean contains(final String filePath) {
        return mUploadedFilePaths.contains(filePath);
    }

    public void saveData(Context context) {
        if (mUploadedFilePaths.size() > 0) {
            try {
                JSONArray jsonArray = new JSONArray();
                Iterator<String> path = mUploadedFilePaths.iterator();
                int index = 0;
                while (path.hasNext()) {
                    jsonArray.put(index++, path.next());
                }
                SharedPreferences sharedPreferences = context.getSharedPreferences(SAVE_NAME, 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("data", jsonArray.toString());
                editor.apply();
                Log.d(this.getClass().getSimpleName(), "save file success size = " + jsonArray.length());
            } catch (Exception e) {
                Log.d(getClass().getSimpleName(), "save data failed");
            }
        }
    }
}
