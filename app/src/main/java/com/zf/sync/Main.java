package com.zf.sync;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.hardware.input.InputManager;
import android.os.IBinder;
import android.view.IWindowManager;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private CommandHandler mCommandHandler;

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
        IWindowManager windowManager = IWindowManager.Stub.asInterface((IBinder) getServiceMethod.invoke(null, "window"));
        InputManager inputManager = (InputManager) InputManager.class.getDeclaredMethod("getInstance", new Class[0]).invoke(null);
        MotionEvent.class.getDeclaredMethod("obtain").setAccessible(true);
        Method injectInputEventMethod = InputManager.class.getMethod("injectInputEvent", InputEvent.class, Integer.TYPE);

        mCommandHandler = new CommandHandler(inputManager, windowManager, injectInputEventMethod);
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
                        Bitmap bitmap = mCommandHandler.screenshot();
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
                                mCommandHandler.handlerDown(line.substring(DOWN.length()));
                            } else if (line.startsWith(MOVE)) {
                                mCommandHandler.handlerMove(line.substring(MOVE.length()));
                            } else if (line.startsWith(UP)) {
                                mCommandHandler.handlerUp(line.substring(UP.length()));
                            } else if (line.startsWith(MENU)) {
                                mCommandHandler.menu();
                            } else if (line.startsWith(HOME)) {
                                mCommandHandler.pressHome();
                            } else if (line.startsWith(BACK)) {
                                mCommandHandler.back();
                            } else if (line.startsWith(DEGREE)) {
                                mCommandHandler.setScale(Float.parseFloat(line.substring(DEGREE.length())) / 100);
                            }
                        } catch (Exception e) {
                            System.out.println(CommandHandler.getTraceInfo(e));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(CommandHandler.getTraceInfo(e));
                }
            }
        }.start();
    }

    private static void writeInt(OutputStream outputStream, int v) throws IOException {
        outputStream.write(v >> 24);
        outputStream.write(v >> 16);
        outputStream.write(v >> 8);
        outputStream.write(v);
    }
}
