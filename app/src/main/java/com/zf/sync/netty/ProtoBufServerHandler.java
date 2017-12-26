package com.zf.sync.netty;

import com.zf.sync.SyncPhone;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ProtoBufServerHandler extends ChannelInboundHandlerAdapter {
    private ChannelHandlerContext mContext;
    private BlockingQueue<SyncPhone.SyncMessage> mTaskQueue = new LinkedBlockingDeque<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        mContext = ctx;

        SyncPhone.SyncMessage.Builder builder = SyncPhone.SyncMessage.newBuilder();
        builder.setDisplaySize(SyncPhone.SyncMessage.Size.newBuilder().setX(1080).setY(720));
        builder.setScreenSize(SyncPhone.SyncMessage.Size.newBuilder().setX(1080).setY(720));
        builder.setHasNavBar(true);
        ctx.writeAndFlush(builder.build());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void addNewMessage(SyncPhone.SyncMessage message){
        mTaskQueue.add(message);

        try {
            SyncPhone.SyncMessage syncMessage = mTaskQueue.take();
            mContext.writeAndFlush(syncMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}