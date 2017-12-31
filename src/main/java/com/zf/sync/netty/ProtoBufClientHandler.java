package com.zf.sync.netty;

import com.google.protobuf.MessageLite;
import com.zf.sync.SyncTool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class ProtoBufClientHandler extends ChannelInboundHandlerAdapter {
    private int index = 0;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof byte[]) {
            byte[] screenshot = (byte[]) msg;

            File file = new File("/Users/zhangfei/netty/" + (index ++) + ".png");
            try {
                file.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(screenshot);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("screen shot length: " + screenshot.length);
        } else if (msg instanceof MessageLite) {
            SyncTool.DeviceInfo message = (SyncTool.DeviceInfo) msg;
            System.out.println("protobuf: " + message.getDisplaySize().getX() + ", " + message.getDisplaySize().getY());
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}