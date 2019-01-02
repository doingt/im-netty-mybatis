package com.maomao.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class WSServerInitialzer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socket) throws Exception {
        ChannelPipeline pipeline = socket.pipeline();
        //websocket 基于http协议 所以要有http编解码器
        pipeline.addLast(new HttpServerCodec());
        //对写大数据流的支持
        pipeline.addLast(new ChunkedWriteHandler());
        //对于httpMessage进行聚合，聚合成fullHttpRequest或者fullHttpResponse
        //几乎在netty编成中都会用到 该Handler
        pipeline.addLast(new HttpObjectAggregator(1024 * 64));

        //=========================以上是用于支持http协议==================================


        //=========================增加心跳支持 start==================================
        pipeline.addLast(new IdleStateHandler(8, 10, 12));
        //针对客服端，如果在1分钟时没有向服务端发送读写心跳(ALL),则主动断开
        //如果读空闲或者写空闲，不处理
        //自定义的空闲检查状态检测
        pipeline.addLast(new HeartBeatHandler());
        //=========================增加心跳支持 end==================================

        /***
         *webSocket 服务器处理协议，用于指定给客服端连接端口的路由；/ws
         * 本handler会帮你处理比较繁琐的事务
         * 会帮你处理握手动作 handshaking(close,ping,pong) ping+pong=心跳
         * 对于webSocket来讲，都得以frames来传输的，不同的数据类型对应的frames也不同
         */

        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
        pipeline.addLast(new ChatHandler());


    }
}

