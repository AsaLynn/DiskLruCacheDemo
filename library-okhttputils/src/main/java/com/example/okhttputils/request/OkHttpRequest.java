package com.example.okhttputils.request;

import android.content.Context;
import android.util.Pair;
import android.widget.ImageView;

import com.example.okhttputils.OkHttpClientManager;
import com.example.okhttputils.callback.ResultCallback;

import java.io.File;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by think on 2017/10/3.
 *
 */

public abstract class OkHttpRequest {
    protected String url;
    protected OkHttpClient mOkHttpClient;
    protected OkHttpClientManager mOkHttpClientManager;
    protected Map<String, String> params;
    protected Map<String, String> headers;
    protected RequestBody requestBody;
    protected Request request;
    protected Context context;

    protected OkHttpRequest(String url, Map<String, String> params,
                            Map<String, String> headers) {
        mOkHttpClientManager = OkHttpClientManager.getInstance();
        mOkHttpClient = mOkHttpClientManager.getOkHttpClient();
        this.url = url;
        this.params = params;
        this.headers = headers;
    }

    protected OkHttpRequest(Context context, String url, Map<String, String> params,
                            Map<String, String> headers) {
        mOkHttpClientManager = OkHttpClientManager.getInstance(context);
        mOkHttpClient = mOkHttpClientManager.getOkHttpClient();
        this.url = url;
        this.params = params;
        this.headers = headers;
        this.context = context;
    }

    protected abstract Request buildRequest();

    protected abstract RequestBody buildRequestBody();

    protected void prepareInvoked(ResultCallback callback) {
        requestBody = buildRequestBody();
        requestBody = wrapRequestBody(requestBody, callback);
        request = buildRequest();
    }



    public void invokeAsyn(ResultCallback callback) {
        prepareInvoked(callback);
        mOkHttpClientManager.execute(request, callback);
    }

    protected RequestBody wrapRequestBody(RequestBody requestBody, final ResultCallback callback) {
        return requestBody;
    }

    public <T> T invoke(Class<T> clazz) throws IOException {
        requestBody = buildRequestBody();
        Request request = buildRequest();
        return mOkHttpClientManager.execute(request, clazz);
    }

    protected void appendHeaders(Request.Builder builder, Map<String, String> headers) {
        if (builder == null) {
            throw new IllegalArgumentException("builder can not be empty!");
        }

        if (headers == null || headers.isEmpty()) {
            return;
        }
        Headers.Builder headerBuilder = new Headers.Builder();

        for (String key : headers.keySet()) {
            headerBuilder.add(key, headers.get(key));
        }
        builder.headers(headerBuilder.build());
    }

    public static class Builder {
        private String url;
        private Map<String, String> headers;
        private Map<String, String> params;
        private Pair<String, File>[] files;
        private MediaType mediaType;

        private String destFileDir;
        private String destFileName;

        private ImageView imageView;
        private int errorResId = -1;
        //for post
        private String content;
        private Context context;
        private byte[] bytes;
        private File file;

        public Builder context(Context context) {
            this.context = context;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder params(Map<String, String> params) {
            this.params = params;
            return this;
        }

        public Builder addParams(String key, String val) {
            if (this.params == null) {
                params = new IdentityHashMap<>();
            }
            params.put(key, val);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder addHeader(String key, String val) {
            if (this.headers == null) {
                headers = new IdentityHashMap<>();
            }
            headers.put(key, val);
            return this;
        }

        public Builder files(Pair<String, File>... files) {
            this.files = files;
            return this;
        }

        public Builder destFileName(String destFileName) {
            this.destFileName = destFileName;
            return this;
        }

        public Builder destFileDir(String destFileDir) {
            this.destFileDir = destFileDir;
            return this;
        }

        public Builder imageView(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        public Builder errResId(int errorResId) {
            this.errorResId = errorResId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder mediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public <T> T get(Class<T> clazz) throws IOException {
            OkHttpRequest request = new OkHttpGetRequest(url, params, headers);
            return request.invoke(clazz);
        }

        public OkHttpRequest get(ResultCallback callback) {
            OkHttpRequest request = new OkHttpGetRequest(url, params, headers);
            request.invokeAsyn(callback);
            return request;
        }

        //post
        public <T> T post(Class<T> clazz) throws IOException {
            OkHttpRequest request = new OkHttpPostRequest(url,params, headers, mediaType, content, bytes, file);
            return request.invoke(clazz);
        }


        public OkHttpRequest post(ResultCallback callback) {
            OkHttpRequest request = new OkHttpPostRequest(url, params, headers, mediaType, content, bytes, file);
            request.invokeAsyn(callback);
            return request;
        }

        public OkHttpRequest upload(ResultCallback callback) {
            OkHttpRequest request = new OkHttpUploadRequest(url,params, headers, files);
            request.invokeAsyn(callback);
            return request;
        }

        public <T> T upload(Class<T> clazz) throws IOException {
            OkHttpRequest request = new OkHttpUploadRequest(url, params, headers, files);
            return request.invoke(clazz);
        }

        public OkHttpRequest download(ResultCallback callback) {
            OkHttpRequest request = new OkHttpDownloadRequest(url, params, headers, destFileName, destFileDir);
            request.invokeAsyn(callback);
            return request;
        }

        public void displayImage(ResultCallback callback) {
            OkHttpRequest request = new OkHttpDisplayImgRequest(url, params, headers, imageView, errorResId);
            request.invokeAsyn(callback);
        }

        public void displayImage() {
            OkHttpRequest request = new OkHttpDisplayImgRequest(url, params, headers, imageView, errorResId);
            request.invokeAsyn(null);
        }

    }

}
