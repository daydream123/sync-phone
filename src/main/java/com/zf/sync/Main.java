package com.zf.sync;

import com.zf.sync.netty.ProtoBufClient;
import com.zf.sync.netty.ProtoBufClientHandler;
import com.zf.sync.utils.CmdUtils;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class Main extends Application {
    private double mTitleBarHeight;
    private BufferedWriter writer;
    private boolean isMove = false;
    private ImageView mScreenImageView;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        MenuBar menuBar = new MenuBar();
        menuBar.prefWidthProperty().bind(primaryStage.widthProperty());
        root.setTop(menuBar);

        Menu menuFile = new Menu("File");
        MenuItem connectMenu = new MenuItem("Connect");
        connectMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                connect(primaryStage, mTitleBarHeight + menuBar.getHeight());
            }
        });
        menuFile.getItems().addAll(connectMenu);

        Menu menuEdit = new Menu("Edit");
        Menu menuView = new Menu("View");
        menuBar.getMenus().addAll(menuFile, menuEdit, menuView);

        mScreenImageView = new ImageView();
        mScreenImageView.setImage(new Image("loading.gif"));
        mScreenImageView.setFitWidth(400);
        mScreenImageView.setFitHeight(600);
        mScreenImageView.setPreserveRatio(true);
        mScreenImageView.setOnMouseClicked(event -> {
            double x = event.getX();
            double y = event.getY();
            try {
                writer.write("DOWN" + (x * 1.0f / mScreenImageView.getFitWidth()) + "#" + (y * 1.0f / mScreenImageView.getFitHeight()));
                writer.newLine();
                writer.write("UP" + (x * 1.0f / mScreenImageView.getFitWidth()) + "#" + (y * 1.0f / mScreenImageView.getFitHeight()));
                writer.newLine();
                writer.flush();
            } catch (Exception ignored) {
            }
        });

        mScreenImageView.setOnMouseReleased(event -> {
            try {
                double x = event.getX();
                double y = event.getY();
                writer.write("UP" + (x * 1.0f / mScreenImageView.getFitWidth()) + "#" + (y * 1.0f / mScreenImageView.getFitHeight()));
                writer.newLine();
                writer.flush();
                isMove = false;
            } catch (Exception ignored) {
            }
        });

        mScreenImageView.setOnMouseMoved(event -> {
            try {
                double x = event.getX();
                double y = event.getY();
                if (!isMove) {
                    isMove = true;
                    writer.write("DOWN" + (x * 1.0f / mScreenImageView.getFitWidth()) + "#" + (y * 1.0f / mScreenImageView.getFitHeight()));
                } else {

                    writer.write("MOVE" + (x * 1.0f / mScreenImageView.getFitWidth()) + "#" + (y * 1.0f / mScreenImageView.getFitHeight()));
                }
                writer.newLine();
                writer.flush();
            } catch (Exception ignored) {
            }
        });

        // 在屏幕上显示图像
        Scene scene = new Scene(root);
        root.setCenter(mScreenImageView);
        primaryStage.setTitle("Sync Tool");
        primaryStage.setScene(scene);
        primaryStage.show();

        mTitleBarHeight = primaryStage.getHeight() - scene.getHeight();
    }

    private void connect(Stage primaryStage, double extraHeight){
        new Thread(){
            @Override
            public void run() {
                try {
                    new ProtoBufClient().connect(8888, "127.0.0.1", new ProtoBufClientHandler.MessageReadListener() {
                        @Override
                        public void onScreenshotsRead(byte[] screenshots) {
                            mScreenImageView.setImage(new Image(new ByteArrayInputStream(screenshots)));
                        }

                        @Override
                        public void onDeviceInfoRead(SyncTool.DeviceInfo deviceInfo) {
                            double height = ((double)deviceInfo.getScreenHeight()) / 4;
                            double width = ((double)deviceInfo.getScreenWidth()) / 4;

                            mScreenImageView.setFitHeight(height);
                            mScreenImageView.setFitWidth(width);
                            primaryStage.setHeight(height + extraHeight);
                            primaryStage.setWidth(width);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void main(String[] args) {
        String apkStorePath = CmdUtils.getApkStorePath("com.zf.sync");
        System.out.println(apkStorePath);
        String export = "export CLASSPATH=" + apkStorePath;
        String execute = "exec app_process $base/bin com.zf.sync.Main '$@'";
        String forwardPort = "adb forward tcp:8888 tcp:8888";

        try {
            Process process = CmdUtils.execShellInWait(export, execute);
            CmdUtils.execCmd(forwardPort);
            CmdUtils.readAndPrint(process.getInputStream());
            launch(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
