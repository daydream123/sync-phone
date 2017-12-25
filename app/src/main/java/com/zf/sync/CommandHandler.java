package com.zf.sync;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.view.InputDeviceCompat;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by zhangfei on 2017/12/20.
 */

public class CommandHandler {
    private InputManager mInputManager;
    private Method mInjectInputEventMethod;
    private IWindowManager mWindowManager;

    private long mDownTime;
    private float mScale = 1;

    public CommandHandler(InputManager inputManager, IWindowManager windowManager, Method injectInputEventMethod) {
        mInputManager = inputManager;
        mWindowManager = windowManager;
        mInjectInputEventMethod = injectInputEventMethod;
    }

    public void handlerUp(String line) {
        Point point = getXY(line);
        if (point != null) {
            try {
                touchUp(point.x, point.y);
            } catch (Exception e) {
                System.out.println(getTraceInfo(e));
            }
        }
    }

    public void handlerMove(String line) {
        Point point = getXY(line);
        if (point != null) {
            try {
                touchMove(point.x, point.y);
            } catch (Exception e) {
                System.out.println(getTraceInfo(e));
            }
        }
    }

    public void handlerDown(String line) {
        Point point = getXY(line);
        if (point != null) {
            try {
                touchDown(point.x, point.y);
            } catch (Exception e) {
                System.out.println(getTraceInfo(e));
            }
        }
    }

    private static Point getXY(String nums) {
        try {
            Point point = SurfaceControlVirtualDisplayFactory.getCurrentDisplaySize(false);
            String[] s = nums.split("#");
            float scaleX = Float.parseFloat(s[0]);
            float scaleY = Float.parseFloat(s[1]);
            point.x *= scaleX;
            point.y *= scaleY;
            return point;
        } catch (Exception e) {
            System.out.println(getTraceInfo(e));
        }
        return null;
    }

    public void menu() throws InvocationTargetException, IllegalAccessException {
        sendKeyEvent(
                mInputManager,
                mInjectInputEventMethod,
                InputDeviceCompat.SOURCE_KEYBOARD,
                KeyEvent.KEYCODE_MENU,
                false);
    }

    public void back() throws InvocationTargetException, IllegalAccessException {
        sendKeyEvent(
                mInputManager,
                mInjectInputEventMethod,
                InputDeviceCompat.SOURCE_KEYBOARD,
                4,
                false);
    }


    public void touchUp(float clientX, float clientY) throws InvocationTargetException, IllegalAccessException {
        injectMotionEvent(
                mInputManager,
                mInjectInputEventMethod,
                InputDeviceCompat.SOURCE_TOUCHSCREEN,
                1,
                mDownTime,
                SystemClock.uptimeMillis(),
                clientX,
                clientY,
                1.0f);
    }

    public void touchMove(float clientX, float clientY) throws InvocationTargetException, IllegalAccessException {
        injectMotionEvent(
                mInputManager,
                mInjectInputEventMethod,
                InputDeviceCompat.SOURCE_TOUCHSCREEN,
                2,
                mDownTime,
                SystemClock.uptimeMillis(),
                clientX,
                clientY,
                1.0f);
    }

    public void touchDown(float clientX, float clientY) throws InvocationTargetException, IllegalAccessException {
        mDownTime = SystemClock.uptimeMillis();
        injectMotionEvent(
                mInputManager,
                mInjectInputEventMethod,
                InputDeviceCompat.SOURCE_TOUCHSCREEN,
                0,
                mDownTime,
                mDownTime,
                clientX,
                clientY,
                1.0f);
    }

    public void pressHome() throws InvocationTargetException, IllegalAccessException {
        sendKeyEvent(
                mInputManager,
                mInjectInputEventMethod,
                InputDeviceCompat.SOURCE_KEYBOARD,
                3,
                false);
    }

    public static void injectMotionEvent(InputManager im, Method injectInputEventMethod,
                                          int inputSource, int action, long downTime,
                                          long eventTime, float x, float y, float pressure)
            throws InvocationTargetException, IllegalAccessException {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, pressure, 1.0f, 0, 1.0f, 1.0f, 0, 0);
        event.setSource(inputSource);
        injectInputEventMethod.invoke(im, event, 0);
    }

    public static void sendKeyEvent(InputManager im, Method injectInputEventMethod,
                                     int inputSource, int keyCode, boolean shift)
            throws InvocationTargetException, IllegalAccessException {
        long now = SystemClock.uptimeMillis();
        int meta = shift ? 1 : 0;

        injectInputEventMethod.invoke(im, new KeyEvent(now, now, 0, keyCode, 0, meta, -1, 0, 0, inputSource), 0);
        injectInputEventMethod.invoke(im, new KeyEvent(now, now, 1, keyCode, 0, meta, -1, 0, 0, inputSource), 0);
    }

    public Bitmap screenshot() throws Exception {
        String surfaceClassName;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            surfaceClassName = "android.view.Surface";
        } else {
            surfaceClassName = "android.view.SurfaceControl";
        }

        Point size = SurfaceControlVirtualDisplayFactory.getCurrentDisplaySize(false);
        size.x *= mScale;
        size.y *= mScale;
        Bitmap bitmap = (Bitmap) Class.forName(surfaceClassName)
                .getDeclaredMethod("screenshot", new Class[]{Integer.TYPE, Integer.TYPE})
                .invoke(null, size.x, size.y);

        int rotation = mWindowManager.getRotation();

        if (rotation == 0) {
            return bitmap;
        }

        Matrix m = new Matrix();
        if (rotation == 1) {
            m.postRotate(-90.0f);
        } else if (rotation == 2) {
            m.postRotate(-180.0f);
        } else if (rotation == 3) {
            m.postRotate(-270.0f);
        }
        return Bitmap.createBitmap(bitmap, 0, 0, size.x, size.y, m, false);
    }

    public void setScale(float scale) {
        this.mScale = scale;
    }

    public static String getTraceInfo(Throwable e) {
        PrintWriter printWriter = null;
        Writer info = new StringWriter();
        try {
            printWriter = new PrintWriter(info);
            e.printStackTrace(printWriter);
            Throwable cause = e.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            return info.toString();
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }
}
