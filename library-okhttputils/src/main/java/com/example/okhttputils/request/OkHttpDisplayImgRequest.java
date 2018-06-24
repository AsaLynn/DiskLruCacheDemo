package com.example.okhttputils.request;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.example.okhttputils.ImageUtils;
import com.example.okhttputils.callback.ResultCallback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by think on 2017/10/3.
 */

public class OkHttpDisplayImgRequest extends OkHttpGetRequest {

    private ImageView imageview;
    private int errorResId;

    protected OkHttpDisplayImgRequest(
            String url, Map<String,
            String> params, Map<String, String> headers,
            ImageView imageView, int errorResId) {
        super(url, params, headers);
        this.imageview = imageView;
        this.errorResId = errorResId;
    }

    private void setErrorResId() {
        mOkHttpClientManager.getDeliveryHandler().post(new Runnable() {
            @Override
            public void run() {
                imageview.setImageResource(errorResId);
            }
        });
    }

    private Response getInputStream() throws IOException {
        Call call = mOkHttpClient.newCall(request);
        return call.execute();
    }

    @Override
    public void invokeAsyn(final ResultCallback callback) {
        prepareInvoked(callback);
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setErrorResId();
                mOkHttpClientManager.
                        sendFailResultCallback(request, e, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = null;
                InputStream is1 = null;
                InputStream is2 = null;
                try {
                    inputStream = response.body().byteStream();
                    if (callback != null) {
                        //--->
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buffer)) > -1) {
                            baos.write(buffer, 0, len);
                        }
                        baos.flush();
                        // 打开一个新的输入流
                        is2 = new ByteArrayInputStream(baos.toByteArray());
                    }
                    //<---


                    ImageUtils.ImageSize actualImageSize = ImageUtils.getImageSize(inputStream);
                    ImageUtils.ImageSize imageViewSize = ImageUtils.getImageViewSize(imageview);
                    int inSampleSize = ImageUtils.calculateInSampleSize(actualImageSize, imageViewSize);
                    try {
                        inputStream.reset();
                    } catch (IOException e) {
                        response = getInputStream();
                        inputStream = response.body().byteStream();
                    }

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = inSampleSize;
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                    mOkHttpClientManager.getDeliveryHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            imageview.setImageBitmap(bitmap);
                        }
                    });
                    if (null != is2) {
                        mOkHttpClientManager.sendSuccessResultCallback(is2, callback);
                    }
                } catch (Exception e) {
                    setErrorResId();
                    mOkHttpClientManager.sendFailResultCallback(request, e, callback);
                } finally {
                    try {
                        if (null != inputStream) {
                            inputStream.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public <T> T invoke(Class<T> clazz) throws IOException {
        return null;
    }
}
