package com.example.readexcel;

import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.List;

import jxl.Sheet;
import jxl.Workbook;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.tv);
        imageView = (ImageView) findViewById(R.id.iv);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());//设置超出屏幕时滑动
        imageView.setImageDrawable(getDrawableByName("ic_launcher"));
        readExcel();
    }

    /**
     * 读excel文件，仅限xls格式文件
     */
    public void readExcel() {
        try {
            AssetManager asset = getAssets();
            InputStream is = asset.open("myExcel.xls");
            Workbook book = Workbook.getWorkbook(is);
            Sheet sheet = book.getSheet(0);//第一张表
            int Rows = sheet.getRows();//总行数
            for (int i = 0; i < Rows; ++i) {
                textView.append(sheet.getCell(0, i).getContents() + ".......");//（列，行） sheet.getCell(0, i).getContents()
                textView.append(sheet.getCell(1, i).getContents() + ".......");
                textView.append(sheet.getCell(2, i).getContents() + "\n");
            }
            book.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * getIdentifier函数，name是名字，第二个参数是资源类型mipmap、drawable，string等，第三个参数是包名
     *
     * @param name
     * @return
     */
    public Drawable getDrawableByName(String name) {
        //No package identifier when getting name for resource number 0x00000000,此报错是名称中有空格，或者其他特殊字符，或者为空。
        //android.content.res.Resources$NotFoundException: Resource ID #0x0 ，此报错是类型调错。图片是mipmap资源，用drawable资源查找，报此错。
//        int resID = getResources().getIdentifier(name, "drawable", getPackageName());//动态拼接图片id
        int resID = getResources().getIdentifier(name, "mipmap", getPackageName());//动态拼接图片id
        return getResources().getDrawable(resID);
    }

    public void onClick(View view) {
        if (view.getId() == R.id.iv) {
            textView.setText("");
            textView.append("点击触发\n");
            readExcel();
        }
    }

    /**
     * api版本6.0及以上，动态权限请求
     */
//    private void requestPermissions(int type, long repaymentTime) {
//        requestPermissions(mNecessaryPermissions, new PermissionsListener() {
//            @Override
//            public void onGranted() {
//                //具备所有权限
//                if (type == 1) {
//                    queryCalendarEvent(repaymentTime);
//                } else if (type == 2) {
//                    addCalendarEvent(repaymentTime);
//                }
//            }
//
//            @Override
//            public void onDenied(List<String> deniedPermissions, boolean isNeverAsk) {
//                if (!isNeverAsk) { //请求权限没有全被勾选不再提示然后拒绝
//                    showPermissionDialog("为了能正常使用\"" + BaseApplication.getApplication().getAppName() + "\"，请授予所需权限", false,
//                            () -> requestPermissions(mNecessaryPermissions, this));
//                } else { //全被勾选不再提示
//                    showPermissionDialog(false);
//                }
//            }
//        });
//    }

    /**
     * 插入日历事件
     */
    private void addCalendarEvent(long repaymentTime) {
        CalendarHelper calendarHelper = CalendarHelper.getInstance();
        long remindTime = calendarHelper.dealTime(repaymentTime);
        calendarHelper.setData("提醒", "备注", remindTime);
        calendarHelper.addCalendarEvent(this, true);
        calendarHelper.setOnCalendarQueryComplete(new CalendarHelper.OnCalendarQueryCompleteListener() {
            @Override
            public void onQueryComplete(boolean isSucceed) {
                if (isSucceed) {
                    Log.d("addCalendarEvent()", "succee");
                }
            }
        });
    }

    /**
     * 查询日历事件
     */
    private void queryCalendarEvent(long repaymentTime) {
        CalendarHelper calendarHelper = CalendarHelper.getInstance();
        long remindTime = calendarHelper.dealTime(repaymentTime);
        calendarHelper.queryCalendarEvent(this, true, remindTime);
        calendarHelper.setOnCalendarQueryComplete(new CalendarHelper.OnCalendarQueryCompleteListener() {
            @Override
            public void onQueryComplete(boolean isSucceed) {
                if (isSucceed) {
                    Log.d("queryCalendarEvent()", "succee");
                } else {
                    Log.d("queryCalendarEvent()", "fail");
                }
            }
        });
    }
}
