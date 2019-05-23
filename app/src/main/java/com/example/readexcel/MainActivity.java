package com.example.readexcel;

import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

import jxl.Sheet;
import jxl.Workbook;

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
}
