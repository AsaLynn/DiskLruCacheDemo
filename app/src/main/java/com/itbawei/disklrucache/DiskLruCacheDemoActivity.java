package com.itbawei.disklrucache;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.okhttputils.OkHttpClientManager;
import com.example.okhttputils.callback.ResultCallback;
import com.example.okhttputils.request.OkHttpRequest;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Request;

public class DiskLruCacheDemoActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView iv_disk;
    private DiskLruCache diskLruCache;
    //图片连接.
    String imageUrl = "http://p4.so.qhimgs1.com/t015276cde9c72fdb94.jpg";
    //http://image-qzone.mamaquan.mama.cn/upload//2014/11/05/545a1da29eeb0.png
    String url = "http://www.weather.com.cn/data/sk/101010100.html";
    private TextView tv1;
    private String TAG = this.getClass().getSimpleName();
    private String itemName = "";
    protected DiskLruCache jsonDiskLruCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dish_lru_cache_demo);
        findViewById(R.id.btn_dish).setOnClickListener(this);
        findViewById(R.id.btn_str).setOnClickListener(this);
        iv_disk = (ImageView) findViewById(R.id.iv_dish);
        tv1 = (TextView) findViewById(R.id.tv1);

        //创建DiskLruCache实例并开启缓存功能.
        getDiskLruCache();
        getJsonDiskLruCache();
    }

    private void getDiskLruCache() {
        //创建缓存目录:包名/cache/bitmap
        File directory = SDCardUtils.getDiskCacheDir(this, "bitmap");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        //应用程序版本号
        int appVersion = getAppVersion(this);

        //缓存数目
        int valueCount = 1;

        //缓存容量:10MB.
        long maxSize = 10 * 1024 * 1024;

        //创建DiskLruCache实例并开启缓存功能
        try {
            //参数:1,缓存路径.2,应用版本号.3,缓存数目.4,缓存的最大值.
            diskLruCache = DiskLruCache.open(directory, appVersion, valueCount, maxSize);
            if (!diskLruCache.isClosed()) {//判断开启或者关闭
                showResult("开启成功!", tv1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showResult("开启失败!" + e.getMessage(), tv1);
        }
    }

    /*//获取缓存路径.
    //参数:1,context.2,名字唯一标识.
    public File getDiskCacheDir(Context context, String uniqueName) {
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
    }*/

    //    3,地获取到当前应用程序的版本号
//    注意:每当版本号改变，缓存路径下存储的所有数据都会被清除掉，因为DiskLruCache认为当应用程序有版本更新的时候，所有的数据都应该从网上重新获取。
    public int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_dish:
                show();
                break;
            case R.id.btn_str:
                showJson();
                break;
        }
    }

    private void showJson() {
        final String[] items = {"0开启缓存功能",
                "1请求json加入缓存",
                "2读取缓存",
                "3移除缓存",
                "4重置文字",
                "5关闭json缓存",
        };
        new AlertDialog.Builder(this).setTitle("DiskLruCache操作Str")
                .setIcon(R.mipmap.ic_launcher)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //i就是数组的索引.
                        itemName = items[i] + "--->***";
                        switch (i) {
                            case 0:
                                //开启缓存功能
                                if (jsonDiskLruCache.isClosed()) {
                                    getJsonDiskLruCache();
                                } else {
                                    showResult("已经开启过了,无须重复开启!", tv1);
                                }
                                break;
                            case 1:
                                //访问网络获取json加入缓存
                                if (!jsonDiskLruCache.isClosed()) {
                                    getJsonSaveToDisk();
                                } else {
                                    showResult("缓存已经关闭,无法加入缓存!", tv1);
                                }
                                break;
                            case 2:
                                //读取缓存
                                if (!jsonDiskLruCache.isClosed()) {
                                    readJson();
                                } else {
                                    showResult("缓存已经关闭,读取失败", tv1);
                                }
                                break;
                            case 3:
                                //移除缓存.
                                if (!jsonDiskLruCache.isClosed()) {
                                    removeJson();
                                } else {
                                    showResult("缓存已经关闭,移除失败", tv1);
                                }
                                break;
                            case 4:
                                //重置
                                showResult("!!!!", tv1);
                                break;
                            case 5:
                                //关闭
                                closeJsonDiskLruCache();
                                break;
                        }
                    }
                }).create().show();
    }

    private void closeJsonDiskLruCache() {
        try {
            if (!jsonDiskLruCache.isClosed()) {
                //关闭缓存.
                jsonDiskLruCache.close();
                showResult("关闭缓存.", tv1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeJson() {
        try {
            String key = MD5Utils.getMD5Result(url);
            if (jsonDiskLruCache.remove(key)) {
                showResult("移除成功", tv1);
            } else {
                showResult("移除失败", tv1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readJson() {
        try {
            DiskLruCache.Snapshot snapshot = jsonDiskLruCache.get(MD5Utils.getMD5Result(url));
            if (null != snapshot) {
                String str = snapshot.getString(0);
                long length = snapshot.getLength(0);
                showResult(str + "大小是" + length + "b", tv1);
            } else {
                showResult("读取缓存不存在!", tv1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getJsonSaveToDisk() {
        new OkHttpRequest.Builder().url(url)
                .get(new ResultCallback<String>() {
                    @Override
                    public void onError(Request request, Exception e) {
                        showResult(e.getMessage(), tv1);
                    }

                    @Override
                    public void onResponse(String response) {
                        showResult(response, tv1);
                        try {
                            DiskLruCache.Editor edit = jsonDiskLruCache.edit(MD5Utils.getMD5Result(url));
                            edit.set(0, response);
                            edit.commit();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void show() {
        //弹一个对话框，分类选择：
        //创建builder对象。
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //设置标题.
        builder.setTitle("DiskLruCache操作");
        //设置标题的图标.
        builder.setIcon(R.mipmap.ic_launcher);
        //设置列表内容,以及点击事件.
        //参数:1,String数组.2,点击事件.
        final String[] items = {"0开启缓存功能",
                "1请求网络图片加入缓存",
                "2读取缓存",
                "3移除缓存",
                "4重置图片", "5缓存大小", "6关闭缓存", "7缓存最大值", "8删除缓存",
                "9缓存路径", "10修改缓存容量"};
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //i就是数组的索引.
                itemName = items[i] + "--->***";
                switch (i) {
                    case 0:
                        //开启缓存功能
                        if (diskLruCache.isClosed()) {
                            getDiskLruCache();
                        } else {
                            showResult("已经开启过了,无须重复开启!", tv1);
                        }
                        break;
                    case 1:
                        //访问网络获取图片加入缓存
                        if (!diskLruCache.isClosed()) {
                            //saveBitmapToDisk();
                            getImageSaveToDisk();
                        } else {
                            showResult("缓存已经关闭,无法加入缓存!", tv1);
                        }
                        break;
                    case 2:
                        //读取缓存,将图片展示到imageview.
                        if (!diskLruCache.isClosed()) {
                            read();
                        } else {
                            showResult("缓存已经关闭,读取失败", tv1);
                        }
                        break;
                    case 3:
                        //移除缓存.
                        if (!diskLruCache.isClosed()) {
                            remove();
                        } else {
                            showResult("缓存已经关闭,移除失败", tv1);
                        }
                        break;
                    case 4:
                        //重置图片
                        iv_disk.setImageResource(R.mipmap.ic_launcher);
                        showResult("重置图片!", tv1);
                        break;
                    case 5:
                        //读取已经缓存大小.
                        showResult(diskLruCache.size() / 1024 + "kb", tv1);
                        break;
                    case 6:
                        //关闭缓存.
                        closeDiskLruCache();
                        break;
                    case 7:
                        //设定的最大极限.
                        showResult(diskLruCache.getMaxSize() / 1024 + "kb", tv1);
                        break;
                    case 8:
                        //删除.
                        delete();

                        break;
                    case 9:
                        //缓存路径.
                        showResult(diskLruCache.getDirectory().getAbsolutePath(), tv1);
                        break;
                    case 10:
                        //修改缓存容量
                        diskLruCache.setMaxSize(20 * 1024 * 1024);
                        showResult("修改缓存容量", tv1);

                }
            }
        });
        builder.create().show();
    }

    private void getImageSaveToDisk() {
        new OkHttpRequest
                .Builder()
                .url(imageUrl)
                .errResId(R.mipmap.ic_launcher)
                .imageView(iv_disk)
                .displayImage(new ResultCallback<InputStream>() {
                    @Override
                    public void onError(Request request, Exception exception) {
                        showResult(exception.getMessage(), tv1);
                    }

                    @Override
                    public void onResponse(InputStream response) {
                        savetoDisk(response);
                    }
                });
    }

    private void savetoDisk(InputStream response) {
        String url = MD5Utils.getMD5Result(imageUrl);
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        DiskLruCache.Editor editor = null;
        try {
            editor = diskLruCache.edit(url);
            OutputStream outputStream = editor.newOutputStream(0);
            //参数1:流,参数:指定字节容量8kb
            in = new BufferedInputStream(response, 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            editor.commit();//提交操作,意味着写入缓存成功.
            showResult("图片加载并写入缓存成功!", tv1);
        } catch (Exception e) {
            try {
                editor.abort();//终止操作
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            showResult("缓存写入失败!" + e.getMessage(), tv1);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void delete() {
        if (!diskLruCache.isClosed()) {
            try {
                diskLruCache.delete();
                showResult("删除全部缓存.", tv1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void read() {
        try {
            String key = MD5Utils.getMD5Result(imageUrl);
            DiskLruCache.Snapshot snapShot = diskLruCache.get(key);
            if (snapShot != null) {
                InputStream is = snapShot.getInputStream(0);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                iv_disk.setImageBitmap(bitmap);
                showResult("缓存读取成功！", tv1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showResult("缓存读取失败！" + e.getMessage(), tv1);
        }
    }

    private void remove() {
        try {
            String key = MD5Utils.getMD5Result(imageUrl);
            if (diskLruCache.remove(key)) {
                showResult("移除成功", tv1);
            } else {
                showResult("移除失败", tv1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveBitmapToDisk() {

        new Thread() {
            final boolean b = false;

            @Override
            public void run() {
                String url = MD5Utils.getMD5Result(imageUrl);
                //参数:url
                try {
                    DiskLruCache.Editor editor = diskLruCache.edit(url);
                    //参数:
                    OutputStream outputStream = editor.newOutputStream(0);
                    if (downloadUrlToStream(imageUrl, outputStream)) {
                        editor.commit();//提交操作,意味着写入缓存成功.
                        DiskLruCacheDemoActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showResult("缓存写入成功!", tv1);
                            }
                        });
                    } else {
                        editor.abort();//终止操作
                        showResult("缓存写入失败!", tv1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    //5,加载网络图片,写入缓存中.
    //参数:1,图片地址.2,缓存的输出流.
    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeDiskLruCache();
        closeJsonDiskLruCache();
    }

    private void closeDiskLruCache() {
        try {
            if (!diskLruCache.isClosed()) {
                //关闭缓存.
                diskLruCache.close();
                showResult("关闭缓存.", tv1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showResult(String result, TextView mTv) {
        String mResult = itemName + result;
        mTv.setText(mResult);
        Toast.makeText(this, mResult, Toast.LENGTH_SHORT).show();
        Log.i(TAG, mResult);
    }

    private void showResult(String result, final TextView mTv, boolean isMainThread) {
        final String mResult = itemName + result;
        if (isMainThread) {
            mTv.setText(mResult);
            Toast.makeText(this, mResult, Toast.LENGTH_SHORT).show();
        } else {
            OkHttpClientManager.getInstance().getDeliveryHandler().post(new Runnable() {
                @Override
                public void run() {
                    mTv.setText(mResult);
                    Toast.makeText(DiskLruCacheDemoActivity.this, mResult, Toast.LENGTH_SHORT).show();
                }
            });
        }
        Log.i(TAG, mResult);
    }

    private void getJsonDiskLruCache() {
        //创建缓存目录:包名/cache/bitmap
        File directory = SDCardUtils.getDiskCacheDir(this, "json");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        //应用程序版本号
        int appVersion = getAppVersion(this);

        //缓存数目
        int valueCount = 1;

        //缓存容量:10MB.
        long maxSize = 10 * 1024 * 1024;

        //创建DiskLruCache实例并开启缓存功能
        try {
            //参数:1,缓存路径.2,应用版本号.3,缓存数目.4,缓存的最大值.
            jsonDiskLruCache = DiskLruCache.open(directory, appVersion, valueCount, maxSize);
            if (!jsonDiskLruCache.isClosed()) {//判断开启或者关闭
                showResult("开启成功!", tv1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showResult("开启失败!" + e.getMessage(), tv1);
        }
    }

}
