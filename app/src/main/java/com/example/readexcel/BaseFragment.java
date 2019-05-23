package com.example.readexcel;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.readexcel.interfaces.PermissionsListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseFragment extends Fragment implements View.OnClickListener {

    private static final int PERMISSIONRE_QUESTCODE = 1;

    protected AppCompatActivity mActivity;

    private long mStartActivityTime = 0;

    private boolean isExist = true;

    private PermissionsListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mActivity = (AppCompatActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        return null;
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        if (System.currentTimeMillis() - mStartActivityTime > 500) {
            mStartActivityTime = System.currentTimeMillis();
            super.startActivity(intent, options);
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        if (System.currentTimeMillis() - mStartActivityTime > 500) {
            mStartActivityTime = System.currentTimeMillis();
            super.startActivityForResult(intent, requestCode, options);
        }
    }

    @Override
    public void onClick(View v) {
    }

    public void setExist(boolean value) {
        isExist = value;
    }

    public boolean isExist() {
        return isExist;
    }

    protected abstract int getLayoutResID();

    protected void initToolbar(View view) {
    }

    protected void initView(View view, Bundle savedInstanceState) {
    }

    public boolean onBackPressed() {
        try {
            for (Fragment fragment : getChildFragmentManager().getFragments()) {
                if (fragment instanceof BaseFragment) {
                    if (((BaseFragment) fragment).onBackPressed()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected String getFragmentTitle() {
        return null;
    }

    /**
     * 请求权限封装
     *
     * @param permissions
     * @param listener
     */
    public void requestPermissions(String[] permissions, PermissionsListener listener) {
        mListener = listener;
        // 需要请求的权限
        List<String> requestPermissions = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(permission);
            }
        }
        if (!requestPermissions.isEmpty() && Build.VERSION.SDK_INT >= 23) {
            requestPermissions(requestPermissions.toArray(new String[requestPermissions.size()]), PERMISSIONRE_QUESTCODE);
        } else if (mListener != null) {
            mListener.onGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONRE_QUESTCODE:
                if (mListener != null) {
                    List<String> deniedPermissions = new ArrayList<>();
                    //当所有拒绝的权限都勾选不再询问，这个值为true,这个时候可以引导用户手动去授权。
                    boolean isNeverAsk = true;
                    for (int i = 0; i < grantResults.length; i++) {
                        int grantResult = grantResults[i];
                        String permission = permissions[i];
                        if (grantResult == PackageManager.PERMISSION_DENIED) {
                            deniedPermissions.add(permissions[i]);
                            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission)) { // 点击拒绝但没有勾选不再询问
                                isNeverAsk = false;
                            }
                        }
                    }
                    if (deniedPermissions.isEmpty()) {
                        try {
                            mListener.onGranted();
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                            mListener.onDenied(Arrays.asList(permissions), true);
                        }
                    } else {
                        mListener.onDenied(deniedPermissions, isNeverAsk);
                    }
                }
                break;
            default:
                break;
        }
    }

    public void showPermissionDialog(boolean isFinish) {
//        showPermissionDialog("", isFinish, null);
    }

    /**
     * 权限被禁止时，弹出提示语
     *
     * @param message       message
     * @param isFinish      isFinish
     * @param rightCallBack rightCallBack
     */
//    public void showPermissionDialog(String message, final boolean isFinish, AlertFragmentDialog.RightClickCallBack rightCallBack) {
//        if (TextUtils.isEmpty(message)) {
//            message = "\"" + BaseApplication.getApplication().getAppName() + "\"缺少必要权限";
//        }
//        AlertFragmentDialog.Builder builder = new AlertFragmentDialog.Builder(mActivity);
//        if (isFinish) {
//            builder.setLeftBtnText("退出")
//                    .setLeftCallBack(() -> mActivity.finish());
//        } else {
//            builder.setLeftBtnText("取消");
//        }
//        if (rightCallBack != null) {
//            builder.setRightCallBack(rightCallBack);
//        } else {
//            builder.setRightCallBack(() -> {
//                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                intent.setData(Uri.parse("package:" + getContext().getPackageName()));
//                startActivity(intent);
//            });
//        }
//        builder.setContent(message + "\n请手动授予\"" + BaseApplication.getApplication().getAppName() + "\"访问您的权限")
//                .setRightBtnText("去设置")
//                .setCancelable(false)
//                .build();
//    }

}