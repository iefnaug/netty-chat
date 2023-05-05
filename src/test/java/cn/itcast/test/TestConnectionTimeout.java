package cn.itcast.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GF
 * @since 2023/4/8
 */
@Slf4j
public class TestConnectionTimeout {


    public static void main(String[] args) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            ChannelFuture channelFuture = new Bootstrap()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler())
                    .connect("localhost", 8080);
            channelFuture.sync().channel().closeFuture().sync();
            log.info("结束");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }



}
