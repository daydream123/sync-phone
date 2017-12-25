package com.zf.sync.netty;

import com.zf.sync.SyncPhone;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ProtoBufServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
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

}