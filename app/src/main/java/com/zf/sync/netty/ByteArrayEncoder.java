package com.zf.sync.netty;

/**
 * Created by zhangfei on 2017/12/30.
 */

import com.google.protobuf.MessageLite;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ByteArrayEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        byte[] header = encodeHeader(msg);

        System.out.print("from client:");
        for (byte b : header) {
            System.out.print(b + ", ");
        }
        System.out.println();
        out.writeBytes(header);
        out.writeBytes(convertToByteArray(msg));
    }

    private byte[] encodeHeader(Object msg) {
        if (msg instanceof byte[]) {
            byte[] bytes = (byte[]) msg;
            return buildHead(DataType.SCREENSHOT, bytes);
        } else if (msg instanceof MessageLite) {
            MessageLite protoBufMsg = (MessageLite) msg;
            byte[] body = protoBufMsg.toByteArray();
            return buildHead(DataType.DEVICE_INFO, body);
        } else {
            throw new IllegalArgumentException("unknown message content");
        }
    }

    private byte[] convertToByteArray(Object msg){
        if (msg instanceof byte[]) {
            return (byte[]) msg;
        } else if (msg instanceof MessageLite) {
            MessageLite protoBufMsg = (MessageLite) msg;
            return protoBufMsg.toByteArray();
        } else {
            throw new IllegalArgumentException("unknown message content");
        }
    }

    private byte[] buildHead(byte dataType, byte[] bodyBytes){
        byte[] bodyLenAsBytes = ByteBuffer.allocate(4).putInt(bodyBytes.length).array();
        int totalSize = bodyLenAsBytes.length + 1;

        byte[] result = new byte[totalSize];
        System.arraycopy(bodyLenAsBytes, 0, result, 0, bodyLenAsBytes.length);

        result[totalSize - 1] = dataType;
        return result;
    }
}