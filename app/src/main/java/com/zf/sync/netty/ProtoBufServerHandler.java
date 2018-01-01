package com.zf.sync.netty;

import android.graphics.Bitmap;
import android.graphics.Point;

import com.zf.sync.CommandHandler;
import com.zf.sync.SyncTool;
import com.zf.sync.screenshot.SurfaceControlVirtualDisplayFactory;

import java.io.ByteArrayOutputStream;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ProtoBufServerHandler extends ChannelInboundHandlerAdapter {
    private CommandHandler mCommandHandler;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("channelRegistered...");
        mCommandHandler = new CommandHandler();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelActive...");

        SyncTool.DeviceInfo.Builder builder = SyncTool.DeviceInfo.newBuilder();
        Point point = SurfaceControlVirtualDisplayFactory.getCurrentDisplaySize(false);
        builder.setScreenHeight(point.y);
        builder.setScreenWidth(point.x);
        builder.setHasNavBar(true);
        ctx.writeAndFlush(builder.build());

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    System.out.println("in thread ...");
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    while (ctx.channel().isActive()) {
                        // screen shot
                        Bitmap bitmap = mCommandHandler.screenshot();
                        outputStream.reset();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 20, outputStream);
                        byte[] screenshotBytes = outputStream.toByteArray();
                        bitmap.recycle();
                        ctx.writeAndFlush(screenshotBytes);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("screenshot error: " + CommandHandler.getTraceInfo(e));
                }
            }
        }.start();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}