package cn.itcast.server.handler;

import cn.itcast.message.GroupChatRequestMessage;
import cn.itcast.message.GroupChatResponseMessage;
import cn.itcast.server.session.GroupSession;
import cn.itcast.server.session.GroupSessionFactory;
import cn.itcast.server.session.SessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;

/**
 * @author GF
 * @since 2023/4/7
 */
@ChannelHandler.Sharable
public class GroupChatRequestMessageHandler extends SimpleChannelInboundHandler<GroupChatRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupChatRequestMessage msg) throws Exception {
        String from = msg.getFrom();
        String groupName = msg.getGroupName();
        String content = msg.getContent();

        Channel channel = SessionFactory.getSession().getChannel(from);

        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        List<Channel> membersChannel = groupSession.getMembersChannel(groupName);
        for (Channel ch : membersChannel) {
            //跳过自己
            if (ch != channel) {
                GroupChatResponseMessage message = new GroupChatResponseMessage(from, content);
                ch.writeAndFlush(message);
            }

        }
    }
}
