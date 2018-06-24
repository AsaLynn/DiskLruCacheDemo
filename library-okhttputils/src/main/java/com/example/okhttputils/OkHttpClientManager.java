package com.example.okhttputils;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.okhttputils.callback.ResultCallback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by think on 2017/9/30.
 * okhttp3的OkHttpClient的封装使用.
 */

public class OkHttpClientManager {
    //定义静态实例常量.
    private static OkHttpClientManager mInstance;
    //定义全局OkHttpClient
    private OkHttpClient okHttpClient;
    //定义Gson
    private final Gson gson;
    //定义Handler用于处理ui.
    private final Handler deliveryHandler;

    private OkHttpClientManager() {
        //设置连接超时,读取超时,写入超时,
        int maxSize = 10 << 20;//2的20次方.10*2的20次方.
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS);
        //以下验证不设置，那么默认就已经设置了验证
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        //构建OkHttpClient对象.
        okHttpClient = builder.build();
        //初始化gson.
        //transient:修饰符,作用：用来标识一个成员变量在序列化子系统中应被忽略。
        //排除被FINAL,TRANSIENT,STATIC修饰的字段.
        gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL,
                        Modifier.TRANSIENT,
                        Modifier.STATIC
                ).create();
        //初始化handler.
        //若是实例化的时候用Looper.getMainLooper()就表示放到主UI线程去处理。
        deliveryHandler = new Handler(Looper.getMainLooper());
    }

    //构造函数私有不对外创建实例.
    private OkHttpClientManager(Context context) {
        this();
        //设置缓存容量,缓存路径
        //缓存路径.
        File directory = SDCardUtils.getDiskCacheDir(context);
        //1tb=1024gb,1gb=1024mb,1mb=1024kb,1kb=1024byte,
        //int maxSize = 10 * 1024 *1024;//10mb,1mb=1024kb=1024*1024byte.
        int maxSize = 10 << 20;//2的20次方.10*2的20次方.
        okHttpClient = okHttpClient.newBuilder().cache(new Cache(directory, maxSize)).build();
    }

    //采用单例的方式返回一个对象实例.
    public static OkHttpClientManager getInstance() {
        if (null == mInstance) {
            synchronized (OkHttpClientManager.class) {
                if (null == mInstance) {
                    mInstance = new OkHttpClientManager();
                }
            }
        }
        return mInstance;
    }

    //采用单例的方式返回一个对象实例.首次加载使用此方法.
    public static OkHttpClientManager getInstance(Context context) {
        if (null == mInstance) {
            synchronized (OkHttpClientManager.class) {
                if (null == mInstance) {
                    mInstance = new OkHttpClientManager(context);
                }
            }
        }
        return mInstance;
    }

    //对外暴露获取部分实例的方法.
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public Handler getDeliveryHandler() {
        return deliveryHandler;
    }

    //对外暴漏执行异步请求的方法.
    public void execute(final Request request, ResultCallback callback) {
        //若传来的callback为null,则用默认的
        if (null == callback) {
            callback = ResultCallback.DEFAULT_RESULT_CALLBACK;
        }
        //另外定义本地变量接收callback
        final ResultCallback resultCallback = callback;
        //执行发送请求之前的逻辑操作.
        resultCallback.onBefore();
        //获取call并执行异步请求.
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //执行请求失败的操作
                sendFailResultCallback(request, e, resultCallback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //执行请求成功后的操作
                //若相应码在400到599之间,则请求服务器失败,
                if (response.code() >= 400 && response.code() <= 599) {
                    try {
                        sendFailResultCallback(request, new RuntimeException(response.body().string()), resultCallback);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    //获取服务器响应的数据
                    String str = response.body().string();
                    if (resultCallback.mType == String.class) {
                        sendSuccessResultCallback(str, resultCallback);
                    } else {
                        Object o = gson.fromJson(str, resultCallback.mType);
                        sendSuccessResultCallback(o, resultCallback);
                    }
                } catch (Exception e) {
                    sendFailResultCallback(response.request(), e, resultCallback);
                }
            }
        });
    }

    //对外暴露请求结果返回对象的同步请求
    public <T> T execute(Request request, Class<T> clazz) throws IOException {
        Response response = okHttpClient.newCall(request).execute();
        String resultStr = response.body().string();
        return gson.fromJson(resultStr, clazz);
    }

    //对外暴露根据Request取消请求的操作。(此方法不够科学！)
//    public void cancleRequest(Request request){
//        okHttpClient.newCall(request).cancel();
//    }

    public void sendSuccessResultCallback(final Object o, final ResultCallback resultCallback) {
        //回调非空校验
        if (null == resultCallback) {
            return;
        }
        //传递数据到主线程
        deliveryHandler.post(new Runnable() {
            @Override
            public void run() {
                resultCallback.onResponse(o);
                //执行请求结束后的逻辑
                resultCallback.onAfter();
            }
        });

    }

    public void sendFailResultCallback(final Request request, final Exception e, final ResultCallback resultCallback) {
        //若resultCallback为null,则return.否则变轨到主线程执行请求失败和请求结束后的逻辑.
        if (null == resultCallback) {
            return;
        }
        deliveryHandler.post(new Runnable() {
            @Override
            public void run() {
                resultCallback.onError(request, e);
                resultCallback.onAfter();
            }
        });
    }


    /**
     * //对外暴漏设置cache的方法
     *
     * @param directory 路径
     * @param maxSize   大小
     */
    public void cache(File directory, long maxSize) {
        //int maxSize = 10 << 20;//2的20次方.10*2的20次方.
        okHttpClient = okHttpClient.newBuilder().cache(new Cache(directory, maxSize)).build();
    }

    /**
     * 对外暴漏设置cache的方法
     * @param context shangxiawen
     */
    public void cacheDeaufalt(Context context) {
        int maxSize = 10 << 20;//2的20次方.10*2的20次方.
        File directory = SDCardUtils.getDiskCacheDir(context);
        okHttpClient = okHttpClient.newBuilder().cache(new Cache(directory, maxSize)).build();
    }

    public void setCertificates(InputStream... certificates) {
        setCertificates(certificates, null, null);
    }

    ////为每种信任材料返回一个信任管理器
    private TrustManager[] prepareTrustManager(InputStream... certificates) {
        if (certificates == null || certificates.length <= 0) return null;
        try {
            //生成一个实现指定证书类型的 CertificateFactory 对象
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            //KeyStore此类表示密钥和证书的存储设施
            //系统将返回默认类型的 keystore 实现
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            //使用给定的 LoadStoreParameter 加载此 keystore
            keyStore.load(null);
            int index = 0;
            for (InputStream certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                //生成一个证书对象，并使用从输入流 inStream 中读取的数据对它进行初始化。
                //certificateFactory.generateCertificate(certificate)
                //将给定可信证书分配给给定别名。
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
                try {
                    if (certificate != null)
                        certificate.close();
                } catch (IOException e)

                {
                }
            }
            TrustManagerFactory trustManagerFactory = null;
            //此类充当基于信任材料源的信任管理器的工厂。每个信任管理器管理特定类型的由安全套接字使用的信任材料。
            // 信任材料是基于 KeyStore 和/或提供者特定的源。
            //TrustManagerFactory.getDefaultAlgorithm()//获取默认的 TrustManagerFactory 算法名称。
            //返回充当信任管理器工厂的
            trustManagerFactory = TrustManagerFactory.
                    getInstance(TrustManagerFactory.getDefaultAlgorithm());
            //用证书授权源和相关的信任材料初始化此工厂
            trustManagerFactory.init(keyStore);
            //为每种信任材料返回一个信任管理器
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            return trustManagers;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    private KeyManager[] prepareKeyManager(InputStream bksFile, String password) {
        try {
            if (bksFile == null || password == null) return null;
            //返回指定类型的 keystore 对象。
            KeyStore clientKeyStore = KeyStore.getInstance("BKS");
            //从给定输入流中加载此KeyStore
            clientKeyStore.load(bksFile, password.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, password.toCharArray());
            return keyManagerFactory.getKeyManagers();

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setCertificates(InputStream[] certificates, InputStream bksFile, String password) {
        try {
            //为每种信任材料返回一个信任管理器
            TrustManager[] trustManagers = prepareTrustManager(certificates);
            //Returns one key manager for each type of key material.
            KeyManager[] keyManagers = prepareKeyManager(bksFile, password);
            SSLContext sslContext = SSLContext.getInstance("TLS");

            MyTrustManager trustManager = new MyTrustManager(chooseTrustManager(trustManagers));
            sslContext.init(keyManagers, new TrustManager[]{trustManager}, new SecureRandom());
//            okHttpClient = okHttpClient.newBuilder().sslSocketFactory(sslContext.getSocketFactory()).build();
            okHttpClient = okHttpClient.newBuilder().sslSocketFactory(sslContext.getSocketFactory(),trustManager).build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    private X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }


    private class MyTrustManager implements X509TrustManager {
        private X509TrustManager defaultTrustManager;
        private X509TrustManager localTrustManager;

        public MyTrustManager(X509TrustManager localTrustManager) throws NoSuchAlgorithmException, KeyStoreException {
            TrustManagerFactory var4 = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            var4.init((KeyStore) null);
            defaultTrustManager = chooseTrustManager(var4.getTrustManagers());
            this.localTrustManager = localTrustManager;
        }


        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException ce) {
                localTrustManager.checkServerTrusted(chain, authType);
            }
        }


        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }


}
