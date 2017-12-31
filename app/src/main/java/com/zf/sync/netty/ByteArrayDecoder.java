package com.zf.sync.netty;

/**
 * Created by zhangfei on 2017/12/30.
 */

import com.zf.sync.SyncTool;

import java.nio.ByteBuffer;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ByteArrayDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() > 5) { // 如果可读长度小于包头长度，退出。
            in.markReaderIndex();
            byte head0 = in.readByte();
            byte head1 = in.readByte();
            byte head2 = in.readByte();
            byte head3 = in.readByte();
            byte[] sizeBytes = {head0, head1, head2, head3};

            int msgLen = ByteBuffer.wrap(sizeBytes).asIntBuffer().get();
            byte dataType = in.readByte();

            // 如果可读长度小于body长度，恢复读指针，退出。
            if (in.readableBytes() < msgLen) {
                in.resetReaderIndex();
                return;
            }

            // 读取body
            ByteBuf bodyByteBuf = in.readBytes(msgLen);

            byte[] array;
            int offset;

            int readableLen = bodyByteBuf.readableBytes();
            if (bodyByteBuf.hasArray()) {
                array = bodyByteBuf.array();
                offset = bodyByteBuf.arrayOffset() + bodyByteBuf.readerIndex();
            } else {
                array = new byte[readableLen];
                bodyByteBuf.getBytes(bodyByteBuf.readerIndex(), array, 0, readableLen);
                offset = 0;
            }

            //反序列化
            Object result = decodeBody(dataType, array, offset, readableLen);
            if (result != null) {
                out.add(result);
            }
        }
    }

    private Object decodeBody(byte dataType, byte[] array, int offset, int length) throws Exception {
        if (dataType == DataType.DEVICE_INFO) {
            return SyncTool.DeviceInfo.getDefaultInstance().
                    getParserForType().parseFrom(array, offset, length);
        } else if (dataType == DataType.SCREENSHOT) {
            return array;
        }

        return null;
    }

}