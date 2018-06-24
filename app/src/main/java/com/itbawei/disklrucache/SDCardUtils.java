package com.itbawei.disklrucache;

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

    //获取缓存路径.
    //参数:1,context.2,名字唯一标识.
    public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        //有sd卡,并且没有被移除.
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            ///sdcard/Android/data/<application package>/cache
            cachePath = context.getExternalCacheDir().getPath();  //有sd卡时候
        } else {
//data/data/<application package>/cache
            cachePath = context.getCacheDir().getPath();  //无sd卡时候
        }
//接着又将获取到的路径和一个uniqueName进行拼接，作为最终的缓存路径返回。
//uniqueName为了对不同类型的数据进行区分而设定的一个唯一值，比如说在网易新闻缓存路径下看到的bitmap、//object等文件夹。
        return new File(cachePath + File.separator + uniqueName);
    }
}
