package com.example.okhttputils.request;

import android.util.Pair;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Created by think on 2017/10/3.
 */

public class OkHttpUploadRequest extends OkHttpPostRequest {

    private Pair<String, File>[] files;

    public OkHttpUploadRequest(String url, Map<String, String> params, Map<String, String> headers, Pair<String, File>[] files) {
        super(url, params, headers, null, null, null, null);
        this.files = files;
    }

    @Override
    protected void validParams() {
        if (params == null && files == null) {
            throw new IllegalArgumentException("params and files can't both null in upload request .");
        }
    }

    @Override
    protected RequestBody buildRequestBody() {

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        addParams(builder, params);

        if (files != null) {
            RequestBody fileBody = null;
            for (int i = 0; i < files.length; i++) {
                Pair<String, File> filePair = files[i];
                String fileKeyName = filePair.first;
                File file = filePair.second;
                String fileName = file.getName();
                fileBody = RequestBody.create(MediaType.parse(guessMimeType(fileName)), file);
                /*builder.addPart(Headers.of("Content-Disposition",
                        "form-data; name=\"" + fileKeyName + "\"; filename=\"" + fileName + "\""),
                        fileBody);*/
                //此方法是3.0版本代替以上2.0的方法,更加简单化.
                builder.addFormDataPart(fileKeyName,fileName,fileBody);
            }
        }

        return builder.build();
    }

    private void addParams(MultipartBody.Builder builder, Map<String, String> params) {
        if (builder == null) {
            throw new IllegalArgumentException("builder can not be null .");
        }

        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                //addPart(@Nullable Headers headers, RequestBody body)
                //Headers of(String... namesAndValues)
                //RequestBody create(@Nullable MediaType contentType, String content)
                /*builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + key + "\""),
                        RequestBody.create(null, params.get(key)));*/
                //此方法是3.0版本代替以上2.0的方法,更加简单化.
                builder.addFormDataPart(key,params.get(key));

            }
        }
    }

    private String guessMimeType(String path) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null) {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }
}
