package com.zf.sync;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Client extends JFrame {
    private JLabel label;
    private boolean isMove = false;

    private Client() throws IOException {
        setLayout(new BorderLayout(0, 0));

        JPanel btnPanel = new JPanel(new BorderLayout(5, 5));
        JButton btn = new JButton("链接");
        btnPanel.add(btn, BorderLayout.CENTER);

        JSlider jSlider = createSlider();
        btnPanel.add(jSlider, BorderLayout.SOUTH);

        JPanel panelContainer = new JPanel(new BorderLayout());
        panelContainer.add(btnPanel, BorderLayout.SOUTH);

        JPanel panelContainer2 = new JPanel(new BorderLayout());
        panelContainer2.add(panelContainer, BorderLayout.NORTH);

        label = new JLabel();

        label.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(panelContainer2, BorderLayout.NORTH);

        add(label, BorderLayout.CENTER);

        add(createTableBar(), BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBounds(360, 20, 350, 600);

        setTitle("屏幕共享");
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    read("127.0.0.1", "8888");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        });


        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                int x = mouseEvent.getX();
                int y = mouseEvent.getY();
                try {
                    writer.write("DOWN" + (x * 1.0f / label.getWidth()) + "#" + (y * 1.0f / label.getHeight()));
                    writer.newLine();
                    writer.write("UP" + (x * 1.0f / label.getWidth()) + "#" + (y * 1.0f / label.getHeight()));
                    writer.newLine();
                    writer.flush();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                super.mouseReleased(mouseEvent);
                try {
                    int x = mouseEvent.getX();
                    int y = mouseEvent.getY();
                    writer.write("UP" + (x * 1.0f / label.getWidth()) + "#" + (y * 1.0f / label.getHeight()));
                    writer.newLine();
                    writer.flush();
                    isMove = false;
                } catch (Exception ignored) {
                }
            }
        });
        label.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                super.mouseDragged(mouseEvent);
                try {
                    int x = mouseEvent.getX();
                    int y = mouseEvent.getY();
                    if (!isMove) {
                        isMove = true;

                        writer.write("DOWN" + (x * 1.0f / label.getWidth()) + "#" + (y * 1.0f / label.getHeight()));
                    } else {

                        writer.write("MOVE" + (x * 1.0f / label.getWidth()) + "#" + (y * 1.0f / label.getHeight()));
                    }
                    writer.newLine();
                    writer.flush();
                } catch (Exception ignored) {
                }
            }
        });
    }

    private JSlider createSlider() {
        int minimum = 30;
        int maximum = 100;
        JSlider slider = new JSlider(minimum, maximum, maximum);

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                try {
                    int v = ((JSlider) changeEvent.getSource()).getValue();
                    writer.write("DEGREE" + v);
                    writer.newLine();
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        return slider;
    }

    private JPanel createTableBar() {
        JPanel bar = new JPanel(new BorderLayout());
        JButton menu = new JButton("menu");
        JButton home = new JButton("home");
        JButton back = new JButton("back");

        bar.add(menu, BorderLayout.WEST);
        bar.add(home, BorderLayout.CENTER);
        bar.add(back, BorderLayout.EAST);

        menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                try {
                    writer.write("MENU");
                    writer.newLine();
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        home.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                try {
                    writer.write("HOME");
                    writer.newLine();
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                try {
                    writer.write("BACK");
                    writer.newLine();
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return bar;

    }

    private BufferedWriter writer;

    private void read(final String ip, final String port) throws IOException {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Socket socket = new Socket(ip, Integer.parseInt(port));
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
                        Image image = ImageIO.read(byteArrayInputStream);
                        label.setIcon(new ScaleIcon(new ImageIcon(image)));
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

    public static void main(String[] args) throws IOException {
        new Client().setVisible(true);

    }
}

