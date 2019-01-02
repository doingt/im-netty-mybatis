package com.maomao.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;

@Component
public class WSServer {

    private static class SingletionWSServer {
        static final WSServer instance = new WSServer();
    }

    public static WSServer getInstance() {
        return SingletionWSServer.instance;
    }

    private EventLoopGroup mainGroup;
    private EventLoopGroup subGroup;
    private ServerBootstrap server;
    private ChannelFuture future;

    public WSServer() {
        mainGroup = new NioEventLoopGroup();
        subGroup = new NioEventLoopGroup();
        server = new ServerBootstrap();
        server.group(mainGroup, subGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new WSServerInitialzer());

    }

    public void start() throws InterruptedException {
        int port = 8088;
        this.future = server.bind(port).sync();

        // 127.0.0.1        只有本机可以访问
        // 10.10.10.101     局域网内可以访问
        // 139.205.18.165   公网都可访问
        // 局域网IP段(10.xxx.xxx.xxx,172.16.xxx.xxx,192.168.xxx.xxx)
        // 139.205.18.165 => 局域网路由器(192.168.1.1) => 192.168.1.21(本机) => 127.xxx.xxx.xxx
        //                                               192.168.1.22(Gen)  => 127.xxx.xxx.xxx
        //   IP映射 139.205.18.165:80 => 192.168.1.21:72
        //   IP映射 139.205.18.165:80 => 192.168.1.22:72

        // 公网路由器() => 139.205.18.165(公司)  =>
        //                139.205.18.166(家里的) =>

        System.err.println("netty websocket server 启动完毕... 端口：" + port);
    }
}
