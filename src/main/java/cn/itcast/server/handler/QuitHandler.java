package cn.itcast.server.handler;

import cn.itcast.server.session.SessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GF
 * @since 2023/4/7
 */
@Slf4j
@ChannelHandler.Sharable
public class QuitHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("{} 已经断开", ctx.channel());
        SessionFactory.getSession().unbind(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("{} 已经异常断开", ctx.channel(), cause);
        SessionFactory.getSession().unbind(ctx.channel());
    }
}
