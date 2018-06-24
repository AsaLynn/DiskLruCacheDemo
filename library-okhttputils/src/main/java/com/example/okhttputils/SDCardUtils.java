package com.example.okhttputils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by think on 2017/10/1.
 */

public class SDCardUtils {

    /**
     * 获取缓存路径.
     * @param context
     * @return
     */
    public static File getDiskCacheDir(Context context) {
        String cachePath;
        //当SD卡存在或者SD卡不可被移除的时候
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            ///sdcard/Android/data/<application package>/cache
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            //--手机自身缓存路径/data/data/<application package>/cache
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath);
    }
}
