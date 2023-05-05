package cn.itcast.client;

import cn.itcast.client.handler.RpcResponseMessageHandler;
import cn.itcast.message.RpcRequestMessage;
import cn.itcast.protocol.MessageCodecSharable;
import cn.itcast.protocol.ProtocolFrameDecoder;
import cn.itcast.protocol.SequenceIdGenerator;
import cn.itcast.server.service.HelloService;
import cn.itcast.server.service.HelloServiceImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * @author GF
 * @since 2023/4/8
 */
@Slf4j
public class RpcClientManager {

    private static volatile Channel channel;

    private static final Object LOCK = new Object();

    public static void main(String[] args) {
        HelloService helloService = getProxyService(HelloService.class);
        helloService.sayHello("张三");

//        Object proxyInstance = Proxy.newProxyInstance(RpcClientManager.class.getClassLoader(), new Class[]{HelloService.class}, new InvocationHandler() {
//            @Override
//            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                return null;
//            }
//        });
//        System.out.println(proxyInstance);
    }

    public static Channel getChannel() {
        if (channel != null) {
            return channel;
        }
        synchronized (LOCK) {
            if (channel != null) {
                return channel;
            }
            initChannel();
            return channel;
        }
    }

    public static <T> T getProxyService(Class<T> serviceClass) {
        Object proxyInstance = Proxy.newProxyInstance(
                RpcClientManager.class.getClassLoader(),
                new Class[]{serviceClass},
                (proxy, method, args) -> {
                    int sequenceId = SequenceIdGenerator.nextId();
                    RpcRequestMessage rpcRequestMessage = new RpcRequestMessage(
                            sequenceId,
                            serviceClass.getName(),
                            method.getName(),
                            method.getReturnType(),
                            method.getParameterTypes(),
                            args
                    );
                    getChannel().writeAndFlush(rpcRequestMessage);

                    Promise<Object> promise = new DefaultPromise<>(channel.eventLoop());
                    RpcResponseMessageHandler.PROMISES.put(sequenceId, promise);
//                    promise.await();
                    promise.await(2, TimeUnit.SECONDS);
                    if (promise.isSuccess()) {
                        return promise.getNow();
                    } else {
                        throw new RuntimeException(promise.cause());
                    }
                }
        );
        return (T) proxyInstance;
    }

    private static void initChannel() {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler();
        MessageCodecSharable MESSAGE_CODEC =  new MessageCodecSharable();
        RpcResponseMessageHandler RPC_HANDLER = new RpcResponseMessageHandler();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ProtocolFrameDecoder());
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(MESSAGE_CODEC);
                    ch.pipeline().addLast(RPC_HANDLER);
                }
            });
            channel = bootstrap.connect("localhost", 8080).sync().channel();
//            ChannelFuture channelFuture = channel.writeAndFlush(new RpcRequestMessage(
//                    1,
//                    "cn.itcast.server.service.HelloService",
//                    "sayHello",
//                    String.class,
//                    new Class[]{String.class},
//                    new Object[]{"张三"}
//            ));
//            channelFuture.addListener(future -> {
//                if (!future.isSuccess()) {
//                    log.error("error: ", future.cause());
//                }
//            });

            channel.closeFuture().addListener(future -> {
                eventLoopGroup.shutdownGracefully();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
