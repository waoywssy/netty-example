package com.wy.example.netty.example723;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * MessagePack编解码测试
 *
 * @author Jacky
 * @version 1.0
 * @create 2016-12-29  19:03
 **/
public class EchoServer {


    public void bind(int port) throws Exception {
        //配置服务端的NIO线程组
        NioEventLoopGroup acceptorGroup = new NioEventLoopGroup();
        NioEventLoopGroup IOGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(acceptorGroup, IOGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //LengthFieldBasedFrameDecoder用于处理半包消息
                    //这样后面的MsgpackDecoder接收的永远是整包消息
                    ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                    ch.pipeline().addLast("msgpack decoder", new MsgpackDecoder());
                    //在ByteBuf之前增加2个字节的消息长度字段
                    ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(2));
                    ch.pipeline().addLast("msgpack encoder", new MsgpackEncoder());
                    ch.pipeline().addLast(new EchoServerHandler(10));
                }
            });
            //绑定端口，同步等待成功
            ChannelFuture f = b.bind(port).sync();
            //等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            acceptorGroup.shutdownGracefully();
            IOGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new EchoServer().bind(8080);
    }
}
