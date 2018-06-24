package com.example.okhttputils.callback;

import com.google.gson.internal.$Gson$Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.Request;

/**
 * Created by think on 2017/10/1.
 *
 */

public abstract class ResultCallback<T> {

    //转化为Gson自己的数据类型
    public final Type mType;

    public ResultCallback() {
        //初始化类型
        mType = getSurperclassTypeParameter(getClass());
    }

    //转化为Gson自己的数据类型
    static Type getSurperclassTypeParameter(Class<?> aClass) {
        //Type 是 Java 编程语言中所有类型的公共高级接口。
        // 它们包括原始类型、参数化类型、数组类型、类型变量和基本类型。
        //返回表示此 Class 所表示的实体（类、接口、基本类型或 void）的直接超类的 Type。
        //如果超类是参数化类型，则返回的 Type 对象必须准确反映源代码中所使用的实际类型参数。
        Type superclass = aClass.getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new RuntimeException("Missing type parameter.");
        }
        //ParameterizedType 表示参数化类型
        ParameterizedType parameterized = (ParameterizedType) superclass;
        //返回表示此类型实际类型参数的 Type 对象的数组。
        //parameterized.getActualTypeArguments()
        //parameterized.getActualTypeArguments()[0]//数组中第一个元素.
        //以$打头的都是Gson提供的静态工具类
        //canonicalize 方法的目的是规范我们在泛型内的参数,并且转化成Gson自己既定的数据结构。
        //之所以要调用canonicalize就是要将它转化为Gson自己的数据类型。
        // 通过这种方式,Gson构建出自己的一个类型树,基本就完成了类型的采取。
        return $Gson$Types.canonicalize(parameterized.getActualTypeArguments()[0]);
    }

    public void onBefore(){

    }

    public void onAfter(){

    }

    public void inProgress(float pregress){

    }

    public abstract void onError(Request request,Exception exception);

    public abstract void onResponse(T response);

    //定义一个默认的回调.
    public static ResultCallback<String> DEFAULT_RESULT_CALLBACK = new ResultCallback<String>(){

        @Override
        public void onError(Request request, Exception exception) {

        }

        @Override
        public void onResponse(String response) {

        }
    };

}
