package com.zf.sync.netty;

import android.graphics.Bitmap;
import android.graphics.Point;

import com.zf.sync.CommandHandler;
import com.zf.sync.SyncTool;
import com.zf.sync.screenshot.SurfaceControlVirtualDisplayFactory;

import java.io.ByteArrayOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspHeaderValues;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspResponseStatuses;
import io.netty.handler.codec.rtsp.RtspVersions;
import io.netty.util.CharsetUtil;

public class ProtoBufServerHandler extends ChannelInboundHandlerAdapter {
    private CommandHandler mCommandHandler;

    ProtoBufServerHandler(){
        System.out.println("channelRegistered...");
        mCommandHandler = new CommandHandler();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
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
                        long start = System.currentTimeMillis();

                        // screen shot
                        Bitmap bitmap = mCommandHandler.screenshot();
                        outputStream.reset();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 20, outputStream);
                        byte[] screenshotBytes = outputStream.toByteArray();
                        bitmap.recycle();
                        ctx.writeAndFlush(screenshotBytes);

                        System.out.println("app cost: " + (System.currentTimeMillis() - start));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("screenshot error: " + CommandHandler.getTraceInfo(e));
                }
            }
        }.start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DefaultHttpRequest) {
            DefaultHttpRequest request = (DefaultHttpRequest) msg;
            FullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.NOT_FOUND);
            if (request.method() == RtspMethods.OPTIONS) {
                response.setStatus(RtspResponseStatuses.OK);
                response.headers().add(RtspHeaderValues.PUBLIC, "DESCRIBE, SETUP, PLAY, TEARDOWN");
                sendAnswer(ctx, request, response);
            } else if (request.method() == RtspMethods.DESCRIBE) {
                ByteBuf buf = Unpooled.copiedBuffer("c=IN IP4 10.5.110.117\r\nm=video 5004 RTP/AVP 96\r\na=rtpmap:96 H264/90000\r\n", CharsetUtil.UTF_8);
                response.setStatus(RtspResponseStatuses.OK);
                response.headers().add(RtspHeaderNames.CONTENT_TYPE, "application/sdp");
                response.headers().add(RtspHeaderNames.CONTENT_LENGTH, buf.writerIndex());
                response.content().writeBytes(buf);
                sendAnswer(ctx, request, response);
            } else if (request.method() == RtspMethods.SETUP) {
                response.setStatus(RtspResponseStatuses.OK);
                String session = String.format("%08x", (int) (Math.random() * 65536));
                response.headers().add(RtspHeaderNames.SESSION, session);
                response.headers().add(RtspHeaderNames.TRANSPORT, "RTP/AVP;unicast;client_port=5004-5005");
                sendAnswer(ctx, request, response);
            } else if (request.method() == RtspMethods.PLAY) {
                response.setStatus(RtspResponseStatuses.OK);
                sendAnswer(ctx, request, response);
            } else {
                System.err.println("Not managed :" + request.method());
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void sendAnswer(ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse rep) {
        final String cseq = req.headers().get(RtspHeaderNames.CSEQ);
        if (cseq != null) {
            rep.headers().add(RtspHeaderNames.CSEQ, cseq);
        }
        final String session = req.headers().get(RtspHeaderNames.SESSION);
        if (session != null) {
            rep.headers().add(RtspHeaderNames.SESSION, session);
        }
        if (!HttpUtil.isKeepAlive(req)) {
            ctx.write(rep).addListener(ChannelFutureListener.CLOSE);
        } else {
            rep.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
            ctx.write(rep);
        }
    }
}