package cn.itcast.server.handler;

import cn.itcast.message.RpcRequestMessage;
import cn.itcast.message.RpcResponseMessage;
import cn.itcast.server.service.HelloService;
import cn.itcast.server.service.ServicesFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.Method;

/**
 * @author GF
 * @since 2023/4/8
 */
@ChannelHandler.Sharable
public class RpcRequestMessageHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage msg) throws Exception {
        RpcResponseMessage responseMessage = new RpcResponseMessage();
        try {
            HelloService service = (HelloService) ServicesFactory.getService(Class.forName(msg.getInterfaceName()));
            Method method = service.getClass().getMethod(msg.getMethodName(), msg.getParameterTypes());
            Object result = method.invoke(service, msg.getParameterValue());
            responseMessage.setReturnValue(result);
            responseMessage.setSequenceId(msg.getSequenceId());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.setExceptionValue(new RuntimeException("远程调用错误: " + e.getMessage()));
        }
        ctx.writeAndFlush(responseMessage);
    }
}
