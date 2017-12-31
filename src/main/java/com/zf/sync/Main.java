package com.zf.sync;

import com.zf.sync.utils.CmdUtils;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class Main extends Application {
    private BufferedWriter writer;
    private boolean isMove = false;
    private ImageView screenImageView;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        MenuBar menuBar = new MenuBar();
        menuBar.prefWidthProperty().bind(primaryStage.widthProperty());
        root.setTop(menuBar);

        Menu menuFile = new Menu("File");
        MenuItem connectMenu = new MenuItem("Connect");
        connectMenu.setOnAction(actionEvent -> start());
        menuFile.getItems().addAll(connectMenu);

        Menu menuEdit = new Menu("Edit");
        Menu menuView = new Menu("View");
        menuBar.getMenus().addAll(menuFile, menuEdit, menuView);

        Image image = new Image("main/resources/Screenshot.png", 1080, 720, true, true);
        screenImageView = new ImageView();
        screenImageView.setImage(image);
        screenImageView.setPreserveRatio(true);
        screenImageView.setOnMouseClicked(event -> {
            double x = event.getX();
            double y = event.getY();
            try {
                writer.write("DOWN" + (x * 1.0f / screenImageView.getFitWidth()) + "#" + (y * 1.0f / screenImageView.getFitHeight()));
                writer.newLine();
                writer.write("UP" + (x * 1.0f / screenImageView.getFitWidth()) + "#" + (y * 1.0f / screenImageView.getFitHeight()));
                writer.newLine();
                writer.flush();
            } catch (Exception ignored) {
            }
        });

        screenImageView.setOnMouseReleased(event -> {
            try {
                double x = event.getX();
                double y = event.getY();
                writer.write("UP" + (x * 1.0f / screenImageView.getFitWidth()) + "#" + (y * 1.0f / screenImageView.getFitHeight()));
                writer.newLine();
                writer.flush();
                isMove = false;
            } catch (Exception ignored) {
            }
        });

        screenImageView.setOnMouseMoved(event -> {
            try {
                double x = event.getX();
                double y = event.getY();
                if (!isMove) {
                    isMove = true;
                    writer.write("DOWN" + (x * 1.0f / screenImageView.getFitWidth()) + "#" + (y * 1.0f / screenImageView.getFitHeight()));
                } else {

                    writer.write("MOVE" + (x * 1.0f / screenImageView.getFitWidth()) + "#" + (y * 1.0f / screenImageView.getFitHeight()));
                }
                writer.newLine();
                writer.flush();
            } catch (Exception ignored) {
            }
        });

        // 在屏幕上显示图像
        Scene scene = new Scene(root);
        root.setCenter(screenImageView);
        primaryStage.setTitle("Image Read Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void start(){
        String apkStorePath = CmdUtils.getApkStorePath("com.zf.sync");
        System.out.println(apkStorePath);
        String export = "export CLASSPATH=" + apkStorePath;
        String execute = "exec app_process $base/bin com.zf.sync.Main '$@'";
        String redirect = "forward tcp:8888 localabstract:sync-tool";

        try {
            Process process = CmdUtils.execShellInWait(export, execute);
            CmdUtils.readAndPrint(process.getInputStream());
            CmdUtils.execCmd(redirect);
            Thread.sleep(2000);
            read("127.0.0.1", 8888);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void read(final String ip, final int port) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Socket socket = new Socket(ip, port);
                    BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    byte[] bytes = null;
                    while (true) {
                        int version = inputStream.read();
                        if (version == -1) {
                            return;
                        }
                        int length = readInt(inputStream);
                        if (bytes == null) {
                            bytes = new byte[length];
                        }
                        if (bytes.length < length) {
                            bytes = new byte[length];
                        }
                        int read = 0;
                        while ((read < length)) {
                            read += inputStream.read(bytes, read, length - read);
                        }
                        InputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                        BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);
                        WritableImage writableImage = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
                        SwingFXUtils.toFXImage(bufferedImage, writableImage);
                        screenImageView.setImage(writableImage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private int readInt(InputStream inputStream) throws IOException {
        int b1 = inputStream.read();
        int b2 = inputStream.read();
        int b3 = inputStream.read();
        int b4 = inputStream.read();

        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    public static void main(String[] args) throws InterruptedException {
        launch(args);
    }
}
