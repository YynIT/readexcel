package com.example.readexcel;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CalendarHelper {
    private static final int MY_INSERT = 0;//插入
    private static final int MY_REMIND = 1;//提醒
    private static final int MY_ADD_USER = 2;//添加账户
    private static final String CBK_COOKIE = "cookie";

    private static String CALENDAR_URL = "content://com.android.calendar/calendars";//日历用户的URL
    private static String CALENDAR_EVENT_URL = "content://com.android.calendar/events";//事件的URL
    private static String CALENDAR_REMINDER_URL = "content://com.android.calendar/reminders";//事件提醒URL

    private static String CALENDARS_NAME = "calendars_name";//日历名称
    private static String CALENDARS_ACCOUNT_NAME = "calendars_account_name";//账户拥有者
    private static String CALENDARS_DISPLAY_NAME = "display_name";//展示给用户的日历名称 display_name

    private String title;//事件的标题
    private String address;//备注
    private long time;//开始时间

    private Context mContext;
    private MyAsyncQueryHandler mAsyncQueryHandler;
    private OnCalendarQueryCompleteListener onCalendarQueryComplete;

    public static CalendarHelper getInstance() {
        return new CalendarHelper();
    }

    private CalendarHelper() {
    }

    public void setOnCalendarQueryComplete(OnCalendarQueryCompleteListener onCalendarQueryComplete) {
        this.onCalendarQueryComplete = onCalendarQueryComplete;
    }

    public interface OnCalendarQueryCompleteListener {
        void onQueryComplete(boolean isSucceed);
    }

    /**
     * 接收AsyncQueryHandler事件（同理handler）
     * onInsertComplete方法
     */
    private class MyAsyncQueryHandler extends AsyncQueryHandler {
        public MyAsyncQueryHandler(ContentResolver cr) {
            super(cr);
        }

        /**
         * 发起数据函数，有以下4个：
         * 查  handler.startQuery(token, cookie, uri, projection, selection, selectionArgs, orderBy);
         * 删  handler.startDelete(token, cookie, uri, selection, selectionArgs);
         * 插  handler.startInsert(token, cookie, uri, initialValues);
         * 更新 handler.startUpdate(token, cookie, uri, values, selection, selectionArgs);
         * 插入数据结果处理函数
         *
         * @param token  令牌 类比onActivityResult的请求码，要跟startxxx的对应
         * @param cookie 你想传给onQueryComplete方法使用的一个对象。(没有的话传递null即可)。类比onActivityResult的setResult的返回值
         * @param uri    进行查询的通用资源标志符，类比onActivityResult收到的结果参数
         */
        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            //判断cookie是否是指定的传入信息，如果不是中止，如果是进行请求码的判断。
            //请求码 0添加日历事件 1回调返回成功 2添加日历账户。在0时会判断是否拿到>0的账户ID，如果没拿到，则重新添加日历账户。
            if (cookie instanceof String) {
                String cbkCookie = cookie.toString();
                //通过判断CBK_COOKIE值，确定是否是下面的0 1 2三种信号。是则继续，否则return
                if (!CBK_COOKIE.equals(cbkCookie)) return;
            }
            switch (token) {
                case MY_ADD_USER://在什么情况下会调？在没有获得用户账号id情况下调用
                    if (uri != null) {
                        addCalendarEvent(mContext, false);    //添加日历事件
                    } else {
                        backResult(false);
                    }
                    break;
                case MY_INSERT://添加日历事件
                    if (uri != null) {
                        addRemind(mContext, uri);
                    } else {
                        backResult(false);
                    }
                    break;
                case MY_REMIND:
                    if (uri != null) {
                        backResult(true);
                    }
                    break;
            }
            super.onInsertComplete(token, cookie, uri);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            //查询结果
            if (cursor.getCount() > 0) {
                backResult(true);
            } else {
                backResult(false);
            }
            super.onQueryComplete(token, cookie, cursor);
        }
    }

    private void backResult(boolean isSuccess) {
        if (onCalendarQueryComplete != null) {
            onCalendarQueryComplete.onQueryComplete(isSuccess);
        }
    }

    //获取用户账号的ID
    private int getUserCalendarAccount(Context context) {
        Cursor userCursor = context.getContentResolver().query(Uri.parse(CALENDAR_URL), null, null, null, null);
        try {
            if (userCursor == null || userCursor.getCount() <= 0) { //查询返回空值
                return -1;
            }
            userCursor.moveToFirst();
            do {
                String aa = userCursor.getString(userCursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME));
                if (!TextUtils.isEmpty(aa) && aa.equals(CALENDARS_ACCOUNT_NAME)) {
                    return userCursor.getInt(userCursor.getColumnIndex(CalendarContract.Calendars._ID));
                }
            } while (userCursor.moveToNext());
            return -1;
        } finally {
            if (userCursor != null) {
                userCursor.close();
            }
        }
    }

    /**
     * 创建用户
     */
    private void addCalendarAccount(Context context) {
        ContentValues myValue = new ContentValues();
        //  日历名称
        myValue.put(CalendarContract.Calendars.NAME, CALENDARS_NAME);
        //  日历账号，为邮箱格式
        myValue.put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME);
        //  账户类型，当ACCOUNT_TYPE为ACCOUNT_TYPE_LOCAL时，可以防止在设备中没有找到账号时日程事件被删除。防止为应用不同的登录账号添加日历账号后，用此日历账号在应用中添加日程事件，然后应用切换账号，日程事件消失。
        myValue.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        //  展示给用户的日历名称
        myValue.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDARS_DISPLAY_NAME);
        //  它是一个表示被选中日历是否要被展示的值。
        //  0值表示关联这个日历的事件不应该展示出来。
        //  而1值则表示关联这个日历的事件应该被展示出来。
        //  这个值会影响CalendarContract.instances表中的生成行。
        myValue.put(CalendarContract.Calendars.VISIBLE, 1);
        //  账户标记颜色
        myValue.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.GREEN);
        //  账户级别
        myValue.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        //  它是一个表示日历是否应该被同步和是否应该把它的事件保存到设备上的值。
        //  0值表示不要同步这个日历或者不要把它的事件存储到设备上。
        //  1值则表示要同步这个日历的事件并把它的事件储存到设备上。
        myValue.put(CalendarContract.Calendars.SYNC_EVENTS, 0);
        //  时区
        myValue.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getID());
        //  账户拥有者
        myValue.put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDARS_ACCOUNT_NAME);
        //0 用户界面不应显示 默认1
//        myValue.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0);

        Uri calendarUri = Uri.parse(CALENDAR_URL);
        calendarUri = calendarUri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")//对自己项目操作的系统日历数据做增删查改
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();
        try {

            if (mAsyncQueryHandler == null) {
                mAsyncQueryHandler = new MyAsyncQueryHandler(context.getContentResolver());
            }
            mAsyncQueryHandler.startInsert(MY_ADD_USER, CBK_COOKIE, calendarUri, myValue);
        } catch (Exception e) {
            backResult(false);
        }
    }

    /**
     * 设置提醒事件
     *
     * @param context
     * @param newEvent
     */
    private void addRemind(Context context, Uri newEvent) {
        final ContentValues values = new ContentValues();
        values.put(CalendarContract.Reminders.EVENT_ID, ContentUris.parseId(newEvent));
        // 不提前提醒
        values.put(CalendarContract.Reminders.MINUTES, 0);
        values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        try {

            if (mAsyncQueryHandler == null) {
                mAsyncQueryHandler = new MyAsyncQueryHandler(context.getContentResolver());
            }
            mAsyncQueryHandler.startInsert(MY_REMIND, CBK_COOKIE, Uri.parse(CALENDAR_REMINDER_URL), values);
        } catch (Exception e) {
            backResult(false);
        }
    }

    /**
     * 添加日历事件数据
     *
     * @param title   事件
     * @param address 备注
     * @param time    开始时间
     */
    public void setData(String title, String address, long time) {
        this.title = title;
        this.address = address;
        this.time = time;
    }

    /**
     * 添加日历事件
     *
     * @param context
     * @param isHasUser 是否有用户 true有，false没有，默认有
     */
    public void addCalendarEvent(Context context, boolean isHasUser) {
        mContext = context;
        // 获取日历账户的id
        int calId = getUserCalendarAccount(context);
        //添加过一次账户之后 ID依然为空直接返回失败
        if (calId < 0 && !isHasUser) {
            backResult(false);
            return;
        }
        //如果ID为空，重新添加日历账户
        if (calId < 0) {
            addCalendarAccount(context);
            return;
        }

        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(time);//设置开始时间
        long start = mCalendar.getTime().getTime();
        mCalendar.setTimeInMillis(start);//设置终止时间
        long end = mCalendar.getTime().getTime();

        final ContentValues event = new ContentValues();
        event.put(CalendarContract.Events.TITLE, title);
        event.put(CalendarContract.Events.DESCRIPTION, address);
        event.put(CalendarContract.Events.CALENDAR_ID, calId);
        event.put(CalendarContract.Events.DTSTART, start);
        event.put(CalendarContract.Events.DTEND, end);
        event.put(CalendarContract.Events.HAS_ALARM, 1);//设置有闹钟提醒
        event.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());  //这个是时区

        try {
            mAsyncQueryHandler = new MyAsyncQueryHandler(context.getContentResolver());
            mAsyncQueryHandler.startInsert(MY_INSERT, CBK_COOKIE, Uri.parse(CALENDAR_EVENT_URL), event);
        } catch (Exception e) {
            backResult(false);
        }
    }

    /**
     * 查询日历事件
     *
     * @param context
     * @param isHasUser 是否有用户 true有，false没有，默认有
     * @param time      查询的时间
     */
    public void queryCalendarEvent(Context context, boolean isHasUser, long time) {
        mContext = context;
        // 获取日历账户的id
        int calId = getUserCalendarAccount(context);
        //添加过一次账户之后 ID依然为空直接返回失败
        if (calId < 0 && !isHasUser) {
            backResult(false);
            return;
        }
        //如果ID为空，重新添加日历账户
        if (calId < 0) {
            addCalendarAccount(context);
            return;
        }
        try {
            //deleted != 1语句过滤掉三星逻辑删除后的日历事件
            mAsyncQueryHandler = new MyAsyncQueryHandler(context.getContentResolver());
            mAsyncQueryHandler.startQuery(MY_INSERT, CBK_COOKIE, Uri.parse(CALENDAR_EVENT_URL),
                    new String[]{CalendarContract.Events.CALENDAR_ID, CalendarContract.Events.DTSTART},
                    CalendarContract.Events.CALENDAR_ID + "=? AND " + CalendarContract.Events.DTSTART + " =? AND deleted != 1",
                    new String[]{calId + "", time + ""}, null);
        } catch (Exception e) {
            backResult(false);
        }
    }

    /**
     * 提前或延后时间
     *
     * @param time
     * @return
     */
    public long dealTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.CHINESE);// HH:mm:ss//获取当前时间
        Date date = new Date(time);
        String endDay = simpleDateFormat.format(date);//时分秒置0
        try {
            date = simpleDateFormat.parse(endDay);
        } catch (Exception e) {
            e.printStackTrace();
        }
        calendar.setTime(date);
        calendar.add(Calendar.DATE, -1);//向前走一天
        long remindTime = calendar.getTimeInMillis() + 20 * 60 * 60 * 1000;
        return remindTime;
    }
}
