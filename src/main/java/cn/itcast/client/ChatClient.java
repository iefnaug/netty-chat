package cn.itcast.client;

import cn.itcast.message.*;
import cn.itcast.protocol.MessageCodecSharable;
import cn.itcast.protocol.ProtocolFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ChatClient {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();

        CountDownLatch WAIT_FOR_LOGIN = new CountDownLatch(1);
        AtomicBoolean LOGIN = new AtomicBoolean(false);
        AtomicBoolean PRINT_MENU = new AtomicBoolean(true);

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {

                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
//                    ch.pipeline().addLast(new IdleStateHandler(0, 3,0));
//                    ch.pipeline().addLast(new ChannelDuplexHandler() {
//                        @Override
//                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//                            IdleStateEvent event = (IdleStateEvent) evt;
//                            if (event.state() == IdleState.WRITER_IDLE) {
//                                log.debug("3s 没有发送数据");
//                                ctx.writeAndFlush(new PingMessage());
//                            }
//                            super.userEventTriggered(ctx, evt);
//                        }
//                    });
                    ch.pipeline().addLast(new ProtocolFrameDecoder());
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(MESSAGE_CODEC);
                    ch.pipeline().addLast("client handler", new ChannelInboundHandlerAdapter() {

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            log.info("client received msg: {}", msg);
                            if (msg instanceof LoginResponseMessage) {
                                LoginResponseMessage message = (LoginResponseMessage) msg;
                                if (message.isSuccess()) {
                                    LOGIN.set(true);
                                }
                                WAIT_FOR_LOGIN.countDown();
                            }
                        }

                        //建立连接后触发
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            new Thread(() -> {
                                Scanner sc = new Scanner(System.in);
                                System.out.println("请输入用户名:");
                                String username = sc.nextLine();
                                System.out.println("请输入密码:");
                                String password = sc.nextLine();

                                LoginRequestMessage message = new LoginRequestMessage(username, password);
                                ctx.writeAndFlush(message);

                                try {
                                    WAIT_FOR_LOGIN.await();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                if (!LOGIN.get()) {
                                    ctx.channel().close();
                                    return;
                                }
                                while (PRINT_MENU.get()) {
                                    System.out.println("==================================");
                                    System.out.println("send [username] [content]");
                                    System.out.println("gsend [group name] [content]");
                                    System.out.println("gcreate [group name] [m1,m2,m3...]");
                                    System.out.println("gmembers [group name]");
                                    System.out.println("gjoin [group name]");
                                    System.out.println("gquit [group name]");
                                    System.out.println("quit");
                                    System.out.println("==================================");
                                    String command = sc.nextLine();
                                    String[] s = command.split(" ");
                                    switch (s[0]) {
                                        case "send":
                                            ctx.writeAndFlush(new ChatRequestMessage(username, s[1], s[2]));
                                            break;
                                        case "gsend":
                                            ctx.writeAndFlush(new GroupChatRequestMessage(username, s[1], s[2]));
                                            break;
                                        case "gcreate":
                                            HashSet<String> members = new HashSet<>(Arrays.asList(s[2].split(",")));
                                            members.add(username);
                                            ctx.writeAndFlush(new GroupCreateRequestMessage(s[1], members));
                                            break;
                                        case "gmembers":
                                            ctx.writeAndFlush(new GroupMembersRequestMessage(s[1]));
                                            break;
                                        case "gjoin":
                                            ctx.writeAndFlush(new GroupJoinRequestMessage(username, s[1]));
                                            break;
                                        case "gquit":
                                            ctx.writeAndFlush(new GroupQuitRequestMessage(username, s[1]));
                                            break;
                                        case "quit":
                                            ctx.channel().close();
                                            PRINT_MENU.set(false);
                                            break;
                                        default:
                                            log.error("invalid command...");
                                            break;
                                    }
                                }

                            }, "system in").start();
                        }
                    });


                }
            });
            Channel channel = bootstrap.connect("localhost", 8080).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("client error", e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
