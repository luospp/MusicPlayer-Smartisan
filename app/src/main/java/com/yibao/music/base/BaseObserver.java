package com.yibao.music.base;

import com.yibao.music.util.LogUtil;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import retrofit2.HttpException;

/**
 * @author Luoshipeng
 * @ Name:   BaseObserver
 * @ Email:  strangermy98@gmail.com
 * @ Time:   2018/9/14/ 17:36
 * @ Des:    TODO
 */
public abstract class BaseObserver<T> implements Observer<T> {
    private static final String TAG = "BaseObserver";
    private Disposable mDisposable;

    @Override
    public void onSubscribe(Disposable d) {
        this.mDisposable = d;
        // 可选择：自动添加到全局 CompositeDisposable（如果有）
    }

    @Override
    public void onNext(T t) {
        // 由子类强制实现
        onSuccess(t);
    }

    @Override
    public void onError(Throwable e) {
        // 1. 统一打印完整堆栈，方便调试
        // 使用 LogUtil.e 避免 LogUtil.d 内部的 getStackTrace() 开销
        LogUtil.e(TAG, "请求发生错误: " + e.getLocalizedMessage());

        // 2. 统一处理 HTTP 错误（如 401 跳转登录）
        if (e instanceof HttpException) {
            int code = ((HttpException) e).code();
            if (code == 401 || code == 403) {
                // 触发全局重新登录逻辑
                // ...
                return;
            }
        }

        // 3. 调用子类的具体错误处理（抽象方法）
        onFailure(e);
    }

    @Override
    public void onComplete() {
        // 可选：释放资源
    }

    // ----------- 强制子类实现的方法 -----------
    public abstract void onSuccess(T data);

    public abstract void onFailure(Throwable e);

    // ----------- 提供给子类的工具方法 -----------
    public boolean isDisposed() {
        return mDisposable != null && mDisposable.isDisposed();
    }

    public void dispose() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }
}