package com.reige.rxjavalearn;


import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.reige.rxjavalearn.entity.LoginRequest;
import com.reige.rxjavalearn.entity.LoginResponse;
import com.reige.rxjavalearn.entity.RegisterRequest;
import com.reige.rxjavalearn.entity.RegisterResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableContainer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {

    private Unbinder unbinder;

    private Context mContext;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
        mContext = this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.clear();
    }

    @OnClick(R.id.bt1)
    public void onClick1(View v) {
        //创建一个上游 Observable：
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                //emitter发射器
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
                emitter.onComplete();
            }
        });
        //创建一个下游
        Observer<Integer> observer = new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e("bt1", "onSubscribe");
            }

            @Override
            public void onNext(Integer value) {
                Log.e("bt1", "" + value);
            }

            @Override
            public void onError(Throwable e) {
                Log.e("bt1", "onError");
            }

            @Override
            public void onComplete() {
                Log.e("bt1", "complete");
            }
        };
        //建立连接
        observable.subscribe(observer);
    }

    @OnClick(R.id.bt2)
    public void onClick2(View v) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
                emitter.onComplete();
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e("bt2", "onSubscribe");
            }

            @Override
            public void onNext(Integer value) {
                Log.e("bt2", "onNext" + value);
            }

            @Override
            public void onError(Throwable e) {
                Log.e("bt2", "onError");
            }

            @Override
            public void onComplete() {
                Log.e("bt2", "onComplete");
            }
        });
    }

    @OnClick(R.id.bt3)
    public void onClick3(View v) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.e("bt3", "emitter 1");
                emitter.onNext(1);
                Log.e("bt3", "emitter 2");
                emitter.onNext(2);
                Log.e("bt3", "emitter 3");
                emitter.onNext(3);
                Log.e("bt3", "emitter complete");
                emitter.onComplete();
                Log.e("bt3", "emitter 4");
                emitter.onNext(4);
            }
        }).subscribe(new Observer<Integer>() {
            private Disposable mDisposable;
            private int i;

            @Override
            public void onSubscribe(Disposable d) {
                Log.e("bt3", "onSubscribe");
                mDisposable = d;
            }

            @Override
            public void onNext(Integer value) {
                Log.e("bt3", "onNext" + value);
                i++;
                if (i == 2) {
                    Log.e("bt3", "dispose");
                    // 切断水管
                    mDisposable.dispose();
                    Log.e("bt3", "idDisposed" + mDisposable.isDisposed());
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e("bt3", "onError");
            }

            @Override
            public void onComplete() {
                Log.e("bt3", "onComplete");
            }
        });
    }

    @OnClick(R.id.bt21)
    public void onClick21(View v) {
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.e("bt21", "Observable thread is :" + Thread.currentThread().getName());
                Log.e("bt21", "emit 1");
                emitter.onNext(1);
            }
        });
        Consumer<Integer> consumer = new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e("bt21", "Observable thread is :" + Thread.currentThread().getName());
                Log.e("bt21", "onNext" + integer);
            }
        };
        observable.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer);
    }

    @OnClick(R.id.bt22)
    public void onClick22(View v) {
        Api api = RetrofitProvider.get().create(Api.class);
        api.login(new LoginRequest()).subscribeOn(Schedulers.io())//在io线程进行网络请求
                .observeOn(AndroidSchedulers.mainThread())//在主线程处理请求结果
                .subscribe(new Observer<LoginResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        //如果在请求的过程中Activity已经退出了, 这个时候如果回到主线程去更新UI, 那么APP肯定就崩溃了, 怎么办呢,
                        // 上一节我们说到了Disposable , 说它是个开关, 调用它的dispose()方法时就会切断水管, 使得下游收不到事件,
                        // 既然收不到事件, 那么也就不会再去更新UI了. 因此我们可以在Activity中将这个Disposable 保存起来,
                        // 当Activity退出时, 切断它即可.
                        mCompositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(LoginResponse value) {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(mContext, "登录失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(mContext, "登录成功", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @OnClick(R.id.bt31)
    public void onClick31(View v) {
        Api api = RetrofitProvider.get().create(Api.class);
        api.register(new RegisterRequest())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<RegisterResponse>() {
                    @Override
                    public void accept(RegisterResponse registerResponse) throws Exception {
                        Toast.makeText(mContext, "注册成功", Toast.LENGTH_SHORT).show();
                        login();//注册成功调用登陆的方法
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Toast.makeText(mContext, "注册失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void login() {
        Api api = RetrofitProvider.get().create(Api.class);
        api.login(new LoginRequest())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<LoginResponse>() {
                    @Override
                    public void accept(LoginResponse loginResponse) throws Exception {
                        Toast.makeText(mContext, "登录成功", Toast.LENGTH_SHORT).show();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Toast.makeText(mContext, "登陆失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @OnClick(R.id.bt32)
    public void onClick32(View v) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
            }
        }).map(new Function<Integer, String>() {
            @Override
            public String apply(Integer integer) throws Exception {
                return "This is result" + integer;
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.e("bt32", s);
            }
        });
    }

    @OnClick(R.id.bt33)
    public void onClick33(View v) {

        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
            }
        }).flatMap(new Function<Integer, ObservableSource<String>>() {
            @Override
            public ObservableSource<String> apply(Integer integer) throws Exception {
                final List<String> list = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    list.add("this is result" + integer);
                }
                return Observable.fromIterable(list).delay(10, TimeUnit.MILLISECONDS);
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.e("bt33", s);
            }
        });

    }

    @OnClick(R.id.bt34)
    public void onClick34(View v){
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
            }
        }).concatMap(new Function<Integer, ObservableSource<String>>() {
            @Override
            public ObservableSource<String> apply(Integer integer) throws Exception {
                final List<String> list = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    list.add("I am value " + integer);
                }
                return Observable.fromIterable(list).delay(10,TimeUnit.MILLISECONDS);
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.d("tb34", s);
            }
        });
    }

    @OnClick(R.id.bt35)
    public void onClick35(View v) {
        final Api api = RetrofitProvider.get().create(Api.class);
        api.register(new RegisterRequest())//发起注册请求
                .subscribeOn(Schedulers.io())//在io线程进行网络请求
                .observeOn(AndroidSchedulers.mainThread())//在主线程处理请求
                .doOnNext(new Consumer<RegisterResponse>() {
                    @Override
                    public void accept(RegisterResponse registerResponse) throws Exception {
                        //根据注册的响应做一些操作
                    }
                })
                .observeOn(Schedulers.io())//回到io线程处理登陆
                .flatMap(new Function<RegisterResponse, ObservableSource<LoginResponse>>() {
                    @Override
                    public ObservableSource<LoginResponse> apply(RegisterResponse registerResponse) throws
                            Exception {
                        return api.login(new LoginRequest());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())//回到主线处理登陆结果
                .subscribe(new Consumer<LoginResponse>() {
                    @Override
                    public void accept(LoginResponse loginResponse) throws Exception {
                        Toast.makeText(mContext, "登陆成功", Toast.LENGTH_SHORT).show();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Toast.makeText(mContext, "登陆失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @OnClick(R.id.bt41)
    public void onClick41(View v){
        Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.e("bt41","emit 1");
                emitter.onNext(1);
                Log.e("bt41","emit 2");
                emitter.onNext(2);
                Log.e("bt41","emit 3");
                emitter.onNext(3);
                Log.e("bt41","emit 4");
                emitter.onNext(4);
                Log.e("bt41","emit complete1");
                emitter.onComplete();
            }
        });
        Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                Log.e("bt41","emit A");
                emitter.onNext("A");
                Log.e("bt41","emit B");
                emitter.onNext("B");
                Log.e("bt41","emit C");
                emitter.onNext("C");
                Log.e("bt41","emit complete");
                emitter.onComplete();
            }
        });
        Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
            @Override
            public String apply(Integer integer, String s) throws Exception {
                return integer+s;
            }
        }).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e("bt41","onSubscribe");
            }

            @Override
            public void onNext(String value) {
                Log.e("bt41","onNext"+value);
            }

            @Override
            public void onError(Throwable e) {
                Log.e("bt41","onError");
            }

            @Override
            public void onComplete() {
                Log.e("bt41","onComplete");
            }

        });
    }



}
