package com.zf.sync;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.view.InputDeviceCompat;
import android.view.IWindowManager;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private InputManager mInputManager;
    private Method mInjectInputEventMethod;
    private IWindowManager mWindowManager;

    private long mDownTime;
    private float mScale = 1;

    public static void main(String[] args) {
        System.out.println("------ now begin --------");
        new Main().startServer();
    }

    private void startServer(){
        ServerSocket serverSocket = null;

        try {
            System.out.println("startServer ...");
            serverSocket = new ServerSocket(8888);
            init();

            System.out.println("listener...");
            Socket socket = serverSocket.accept();
            acceptConnect(socket);
        } catch (IOException
                | InvocationTargetException
                | NoSuchMethodException
                | ClassNotFoundException
                | IllegalAccessException e) {
            IOUtils.closeQuietly(serverSocket);
            System.out.println("error occurred when start server: " + e.getMessage());
        }
    }

    @SuppressLint("PrivateApi")
    private void init() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getServiceMethod = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
        mWindowManager = IWindowManager.Stub.asInterface((IBinder) getServiceMethod.invoke(null, "window"));
        mInputManager = (InputManager) InputManager.class.getDeclaredMethod("getInstance", new Class[0]).invoke(null);
        MotionEvent.class.getDeclaredMethod("obtain").setAccessible(true);
        mInjectInputEventMethod = InputManager.class.getMethod("injectInputEvent", InputEvent.class, Integer.TYPE);
    }

    private void acceptConnect(Socket socket) {
        System.out.println("accepted...");
        read(socket);
        write(socket);
    }

    private void write(final Socket socket) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
                    while (true) {
                        Bitmap bitmap = screenshot();
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

                        outputStream.write(2);
                        writeInt(outputStream, byteArrayOutputStream.size());
                        outputStream.write(byteArrayOutputStream.toByteArray());
                        outputStream.flush();
                        System.out.println("write screenshot...");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static String getTraceInfo(Throwable e) {
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

    private void read(final Socket socket) {
        new Thread() {
            private final String DOWN = "DOWN";
            private final String MOVE = "MOVE";
            private final String UP = "UP";

            private final String MENU = "MENU";
            private final String HOME = "HOME";
            private final String BACK = "BACK";

            private final String DEGREE = "DEGREE";

            @Override
            public void run() {
                super.run();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    while (true) {
                        try {
                            String line = reader.readLine();
                            if (line == null) {
                                return;
                            }

                            System.out.println("read: " + line);
                            if (line.startsWith(DOWN)) {
                                handlerDown(line.substring(DOWN.length()));
                            } else if (line.startsWith(MOVE)) {
                                handlerMove(line.substring(MOVE.length()));
                            } else if (line.startsWith(UP)) {
                                handlerUp(line.substring(UP.length()));
                            } else if (line.startsWith(MENU)) {
                                menu();
                            } else if (line.startsWith(HOME)) {
                                pressHome();
                            } else if (line.startsWith(BACK)) {
                                back();
                            } else if (line.startsWith(DEGREE)) {
                                mScale = Float.parseFloat(line.substring(DEGREE.length())) / 100;
                            }
                        } catch (Exception e) {
                            System.out.println(getTraceInfo(e));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(getTraceInfo(e));
                }
            }
        }.start();
    }

    private void handlerUp(String line) {
        Point point = getXY(line);
        if (point != null) {
            try {
                touchUp(point.x, point.y);
            } catch (Exception e) {
                System.out.println(getTraceInfo(e));
            }
        }
    }

    private void handlerMove(String line) {
        Point point = getXY(line);
        if (point != null) {
            try {
                touchMove(point.x, point.y);
            } catch (Exception e) {
                System.out.println(getTraceInfo(e));
            }
        }
    }

    private void handlerDown(String line) {
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


    private static void writeInt(OutputStream outputStream, int v) throws IOException {
        outputStream.write(v >> 24);
        outputStream.write(v >> 16);
        outputStream.write(v >> 8);
        outputStream.write(v);
    }

    private Bitmap screenshot() throws Exception {
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

    private void menu() throws InvocationTargetException, IllegalAccessException {
        sendKeyEvent(
                mInputManager,
                mInjectInputEventMethod,
                InputDeviceCompat.SOURCE_KEYBOARD,
                KeyEvent.KEYCODE_MENU,
                false);
    }

    private void back() throws InvocationTargetException, IllegalAccessException {
        sendKeyEvent(
                mInputManager,
                mInjectInputEventMethod,
                InputDeviceCompat.SOURCE_KEYBOARD,
                4,
                false);
    }


    private void touchUp(float clientX, float clientY) throws InvocationTargetException, IllegalAccessException {
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

    private void touchMove(float clientX, float clientY) throws InvocationTargetException, IllegalAccessException {
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

    private void touchDown(float clientX, float clientY) throws InvocationTargetException, IllegalAccessException {
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


    private void pressHome() throws InvocationTargetException, IllegalAccessException {
        sendKeyEvent(
                mInputManager,
                mInjectInputEventMethod,
                InputDeviceCompat.SOURCE_KEYBOARD,
                3,
                false);
    }

    private static void injectMotionEvent(InputManager im, Method injectInputEventMethod,
                                          int inputSource, int action, long downTime,
                                          long eventTime, float x, float y, float pressure)
            throws InvocationTargetException, IllegalAccessException {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, pressure, 1.0f, 0, 1.0f, 1.0f, 0, 0);
        event.setSource(inputSource);
        injectInputEventMethod.invoke(im, event, 0);
    }

    private static void sendKeyEvent(InputManager im, Method injectInputEventMethod,
                                     int inputSource, int keyCode, boolean shift)
            throws InvocationTargetException, IllegalAccessException {
        long now = SystemClock.uptimeMillis();
        int meta = shift ? 1 : 0;

        injectInputEventMethod.invoke(im, new KeyEvent(now, now, 0, keyCode, 0, meta, -1, 0, 0, inputSource), 0);
        injectInputEventMethod.invoke(im, new KeyEvent(now, now, 1, keyCode, 0, meta, -1, 0, 0, inputSource), 0);
    }

}
