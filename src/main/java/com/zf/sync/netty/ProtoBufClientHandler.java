package com.zf.sync.netty;

import com.google.protobuf.MessageLite;
import com.zf.sync.SyncTool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class ProtoBufClientHandler extends ChannelInboundHandlerAdapter {
    private MessageReadListener mListener;
    public ProtoBufClientHandler(MessageReadListener listener) {
        mListener = listener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try{
            if (msg instanceof byte[]) {
                byte[] screenshots = (byte[]) msg;
                if (mListener != null) {
                    mListener.onScreenshotsRead(screenshots);
                }
            } else if (msg instanceof MessageLite) {
                SyncTool.DeviceInfo info = (SyncTool.DeviceInfo) msg;
                if (mListener != null) {
                    mListener.onDeviceInfoRead(info);
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public interface MessageReadListener {
        void onScreenshotsRead(byte[] screenshots);

        void onDeviceInfoRead(SyncTool.DeviceInfo deviceInfo);
    }

}