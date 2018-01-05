package com.zf.sync;

import android.annotation.SuppressLint;

import com.zf.sync.netty.ProtoBufServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class Main {
    private CommandHandler mCommandHandler;

    public static void main(String[] args) {
        System.out.println("------ now begin --------");
        new Main().startServer();
    }

    private void startServer(){
        ProtoBufServer server = new ProtoBufServer();
        try {
            init();
            server.bind(8888);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("------ error occurred --------");
        }
    }

    @SuppressLint("PrivateApi")
    private void init() {
        mCommandHandler = new CommandHandler();
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
}
