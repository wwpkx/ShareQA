package com.bfzs.smartapplication.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.bfzs.smartapplication.BuildConfig;
import com.bfzs.smartapplication.app.MyApp;
import com.bfzs.smartapplication.ui.upgrade.InstallApkUtil;
import com.bfzs.smartapplication.ui.upgrade.UpgradeManager;
import com.bfzs.smartapplication.utils.PreferenceUtil;
import com.bfzs.smartapplication.utils.StatusBarUtils;
import com.bfzs.smartapplication.utils.SystemStatusManager;
import com.bfzs.smartapplication.utils.ToastUtil;
import com.bfzs.smartapplication.utils.help.ToolbarHelper;
import com.bfzs.smartapplication.utils.logger.LogLevel;
import com.bfzs.smartapplication.utils.logger.Logger;
import com.bfzs.smartapplication.widget.LoadProgress;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.PushAgent;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;

/**
 * Created by Samuel.shang on 2018/8/6.
 */
public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = BaseActivity.class.getSimpleName();
    //标题头
    protected ToolbarHelper helper;
    protected Activity context;

    protected PreferenceUtil preferenceUtil;
    protected LoadProgress loadProgress;
    protected InputMethodManager inputMethodManager;
    protected LayoutInflater mInflater;

    //以下如果需要则需要在setContentView 设置
    //是否使用更换主题之后的状态栏
    protected boolean isStatusBarDark = true;
    protected boolean isShowTitleHelper = true;
    //设置是否显示悬浮标题头
    //默认显示  改为true 的时候则不显示
    protected boolean overly = false;

    public Intent intent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        //统计应用启动数据
        PushAgent.getInstance(this).onAppStart();

        if (isShowTitleHelper) {
            setTranslucentStatus();
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        if (BuildConfig.DEBUG) {
            Logger.init(getClass().getSimpleName()).setLogLevel(LogLevel.FULL).hideThreadInfo();
        } else {
            Logger.init(getClass().getSimpleName()).setLogLevel(LogLevel.NONE).hideThreadInfo();
        }

        if (isStatusBarDark) {
            StatusBarUtils.setSystemUiVisibility(this.getWindow());
            StatusBarUtils.MIUISetStatusBarLightMode(this.getWindow(), true);
            StatusBarUtils.FlymeSetStatusBarLightMode(this.getWindow(), true);
        }
        if (isShowTitleHelper) {
            helper = new ToolbarHelper(context, layoutResID, overly);
            setContentView(helper.getContentView());
            //默认显示标题栏
            isShowTitleLayout(true);
            //默认显示返回按钮
            isShowLeftButton(true);
            helper.mLeftImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            helper.mLeftTest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        } else {
            super.setContentView(layoutResID);
        }

        ButterKnife.bind(this);

        loadProgress = new LoadProgress(this);
        //将继承BaseActivity的activity加入到集合里面
        ((MyApp) getApplication()).addActivity(this);
        preferenceUtil = PreferenceUtil.getInstance(context);
        mInflater = LayoutInflater.from(this);


        try {
            initView();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d(TAG, "setContentView: " + "这里View解析错误");
        }
        try {
            initData();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d(TAG, "setContentView: " + "这里数据解析错误");
        }
    }

    /**
     * 初始化View
     *
     * @throws Exception
     */
    protected abstract void initView() throws Exception;

    /**
     * 初始化数据
     *
     * @throws Exception
     */
    protected abstract void initData() throws Exception;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case InstallApkUtil.GET_UNKNOWN_APP_SOURCES: {
                    String downloadApkPath = UpgradeManager.downloadTempPath() + UpgradeManager.downloadTempName(this);
                    File apkFile = new File(downloadApkPath);
                    if (apkFile.exists()) {
                        InstallApkUtil.install(this, downloadApkPath);
                    }
                }
                break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {

    }

    /**
     * 显示软键盘
     */
    public void showKeyboard() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }, 100);
    }

    /**
     * 隐藏软键盘
     */
    protected void hideSoftKeyboard() {
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getCurrentFocus() != null)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 设置状态栏背景状态
     */
    protected void setTranslucentStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window win = getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            winParams.flags |= bits;
            win.setAttributes(winParams);
        }
        SystemStatusManager tintManager = new SystemStatusManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintResource(0);//状态栏无背景
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((MyApp) getApplication()).removeActivity(this);
        if (loadProgress != null) {
            loadProgress.cancelDialog();
        }
        System.gc();
    }


    /********************************标题部分**************************************/
    /**
     * 左边的标题
     *
     * @param resId
     */
    public void setLeftTest(int resId) {
        helper.mLeftImage.setVisibility(View.GONE);
        helper.mLeftTest.setVisibility(View.VISIBLE);
        helper.mLeftTest.setText(resId);
    }

    /**
     * 左边的标题
     *
     * @param leftTitle
     */
    public void setLeftTest(String leftTitle) {
        helper.mLeftImage.setVisibility(View.GONE);
        helper.mLeftTest.setVisibility(View.VISIBLE);
        helper.mLeftTest.setText(leftTitle);
    }

    /**
     * 设置返回按钮的监听事件
     *
     * @param listener
     */
    public void setLeftImageListener(View.OnClickListener listener) {
        helper.mLeftImage.setOnClickListener(listener);
    }

    /**
     * 设置没有标题头
     *
     * @param layoutResID
     */
    public void setTitleHead(int layoutResID) {
        helper.getContentView().removeAllViews();
        View view = getLayoutInflater().inflate(layoutResID, null);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        helper.getContentView().addView(view, params);
        setContentView(helper.getContentView());
    }

    /**
     * 设置返回按钮的图标
     *
     * @param imageResID
     */
    public void setLeftButton(int imageResID) {
        helper.mLeftImage.setImageResource(imageResID);
    }

    /**
     * 左边的点击事件
     *
     * @param listener
     */
    public void setOnLeftListener(View.OnClickListener listener) {
        helper.mLeftTest.setOnClickListener(listener);
    }

    /**
     * 是否显示左边的图标
     *
     * @param isShow
     */
    public void isShowLeftButton(boolean isShow) {
        if (isShow) {
            helper.mLeftImage.setVisibility(View.VISIBLE);
        } else {
            helper.mLeftImage.setVisibility(View.GONE);
        }
    }

    /**
     * 设置标题资源
     *
     * @param titleName
     */
    public void setTitle(String titleName) {
        helper.mTitleNameTest.setText(titleName);
    }

    /**
     * 设置标题资源
     *
     * @param titleResID
     */
    public void setTitle(int titleResID) {
        helper.mTitleNameTest.setText(titleResID);
    }


    /**
     * 设置标题栏颜色
     *
     * @param resId
     */
    public void setTitleTextColor(int resId) {
        helper.mTitleNameTest.setTextColor(ContextCompat.getColor(this, resId));
    }

    /**
     * 设置右边按钮的文字
     *
     * @param rightTitleName
     */
    public void setRightTitle(String rightTitleName) {
        helper.mRightTitleTest.setVisibility(View.VISIBLE);
        helper.mRightImage.setVisibility(View.GONE);
        helper.mRightTitleTest.setText(rightTitleName);
    }

    /**
     * 设置右边按钮的文字
     *
     * @param rightTitleResID
     */
    public void setRightTitle(int rightTitleResID) {
        helper.mRightTitleTest.setVisibility(View.VISIBLE);
        helper.mRightImage.setVisibility(View.GONE);
        helper.mRightTitleTest.setText(rightTitleResID);
    }

    /**
     * 设置右边文字的点击事件
     *
     * @param listener
     */
    public void setRightListener(View.OnClickListener listener) {
        helper.mRightTitleTest.setOnClickListener(listener);
    }

    public void setRightTextColor(int res) {
        helper.mRightTitleTest.setTextColor
                (ContextCompat.getColor(this, res));
    }

    /**
     * 设置右边文字的大小
     *
     * @param size
     */
    public void setRightSize(int size) {
        helper.mRightTitleTest.setTextSize(size);
    }

    /**
     * 设置左边字大小
     *
     * @param size
     */
    public void setLeftSize(int size) {
        helper.mLeftTest.setTextSize(size);
    }

    /**
     * 设置右边图片
     *
     * @param rightImageResID
     */
    public void setRightImage(int rightImageResID) {
        helper.mRightImage.setVisibility(View.VISIBLE);
        helper.mRightTitleTest.setVisibility(View.GONE);
        helper.mRightImage.setImageResource(rightImageResID);
    }

    /**
     * 设置背景颜色
     *
     * @param res
     */
    public void setTitleBackgroundResource(int res) {
        helper.mTitleLayout.setBackgroundResource(res);
    }

    /**
     * 设置右边图片的点击事件
     *
     * @param listener
     */
    public void setRightImageListener(View.OnClickListener listener) {
        helper.mRightImage.setOnClickListener(listener);
    }


    /**
     * 设置定义的标题
     *
     * @param layoutResID
     */
    public void setTitleLayout(int layoutResID) {
        helper.mTitleLayout.removeAllViews();
        View view = getLayoutInflater().inflate(layoutResID, null);
        helper.mTitleLayout.addView(view);
    }

    /**
     * 是否显示标题栏
     *
     * @param isShow
     */
    public void isShowTitleLayout(boolean isShow) {
        if (isShow) {
            helper.mTitleLayout.setVisibility(View.VISIBLE);
        } else {
            helper.mTitleLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 是否显示标题的下划线
     *
     * @param isShow
     */
    public void isShowTitleLine(boolean isShow) {
        if (isShow) {
            helper.title_line.setVisibility(View.VISIBLE);
        } else {
            helper.title_line.setVisibility(View.GONE);
        }
    }

    public void setStatusBarDark(boolean statusBarDark) {
        isStatusBarDark = statusBarDark;
    }

    public void setShowTitleHelper(boolean showTitleHelper) {
        isShowTitleHelper = showTitleHelper;
    }

    /********************************标题部分**************************************/


    /********************************Toast部分**************************************/
    public void showSuccessToast(String res) {
        ToastUtil.showToast(this, res, ToastUtil.ToastType.SUCCESS);
    }

    public void showSuccessToast(int res) {
        ToastUtil.showToast(this, res, ToastUtil.ToastType.SUCCESS);
    }

    public void showFailureToast(String res) {
        ToastUtil.showToast(this, res, ToastUtil.ToastType.FAILURE);
    }

    public void showFailureToast(int res) {
        ToastUtil.showToast(this, res, ToastUtil.ToastType.FAILURE);
    }

    public void showLoad() {
        loadProgress.showDialog();
    }

    public void disLoad() {
        loadProgress.cancelDialog();
    }

    public boolean enableLoad() {
        return true;
    }
    /********************************Toast部分**************************************/


    /*******************************网络请求部分**********************************/
    public void onSuccess() {
        if (enableLoad()) {
            disLoad();
        }
    }

    public void onFailure(String errorMsg) {
        if (enableLoad()) {
            disLoad();
        }
        showFailureToast(errorMsg);
    }

    /**
     * 网络请求
     */
    public void request() {

    }

    public void load() {
        if (enableLoad()) {
            showLoad();
        }
        request();
    }
    /*******************************网络请求部分**********************************/

}
