package com.maomao.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maomao.SpringUtil;
import com.maomao.enums.MsgActionEnum;
import com.maomao.service.UserService;
import com.maomao.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * 处理消息的handler
 * TextWebSocketFrame 在netty中是用于处理文本对象的，frame是消息的载体
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    public Logger logger = LoggerFactory.getLogger(ChatHandler.class);
    /**
     * 用于记录和管理所有的channel
     */
    public static ChannelGroup users =
            new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg)
            throws Exception {
        /**从客户端接收到的消息**/
        String content = msg.text();
        logger.info(content);
        Channel contchannel = ctx.channel();
        /**1 获取客服端发来的消息**/
        Datecontent dataContent =JsonUtils.jsonToPojo(content, Datecontent.class);

        if (dataContent == null) {
            return;
        }
        Integer action = dataContent.getAction();
        /**2 判断消息类型，根据不同的类型来处理业务**/
        if (action.equals(MsgActionEnum.CONNECT.type)) {
            /**2.1 当websocket 第一次open的时候，初始化channel，把用的channel和userid关联起来**/
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannelRel.put(senderId, contchannel);
            // 测试
            for (Channel c : users) {
                System.out.println(c.id().asLongText());
            }
            UserChannelRel.output();
        } else if (action.equals(MsgActionEnum.CHAT.type)) {
            /**2.2  聊天类型的消息，把聊天记录保存到数据库，同时标记消息的签收状态[未签收]**/
            ChatMsg chatmsg = dataContent.getChatMsg();
            String Msg = chatmsg.getMsg();
            String receiverid = chatmsg.getReceiverId();
            String senderid = chatmsg.getSenderId();
            // 保存消息到数据库，并且标记未签收
            UserService userService = (UserService) SpringUtil.getBean("userServerImpl");
            String msgId = userService.saveMsg(chatmsg);
            chatmsg.setMsgId(msgId);

            //发送消息
            //从全局用户Channel中获取接受方的Channel;
            Channel receiverChannel = UserChannelRel.get(receiverid);
            Datecontent dataContentMsg = new Datecontent();
            dataContentMsg.setChatMsg(chatmsg);
            if (receiverChannel == null) {
                //TODO channel为空代表用户离线，推送消息（推送消息，第三方工具）
            } else {
                //receiverChannel不为空的时候，从ChannelGroup去查找对应的Channel是否存在
                Channel findChannel = users.find(receiverChannel.id());
                if (findChannel != null) {
                    // 用户在线
                    receiverChannel.writeAndFlush(
                            new TextWebSocketFrame(
                                    JsonUtils.objectToJson(dataContentMsg)));
                } else {
                    // 用户离线 TODO 推送消息
                }

            }


        } else if (action.equals(MsgActionEnum.SIGNED.type)) {
            /**2.3 签到消息类型。针对具体的消息进行处理，修改数据库对应的消息状态【已签收】**/
            UserService userService = (UserService) SpringUtil.getBean("userServerImpl");
            //扩展字段在signed类型消息中，代表需要签收的消息id,逗号隔开，
            String msgidstr = dataContent.getExtand();
            String msgids[] = msgidstr.split(",");
            List<String> msgidlist = new ArrayList<>();
            for (String mid : msgids) {
                if (StringUtils.isBlank(mid)) {
                    msgidlist.add(mid);
                    System.out.println(msgidlist.toString());
                    if (msgidlist != null && !msgidlist.isEmpty() && msgidlist.size() > 0) {
                        //批量接受消息
                        userService.updateMsgSigned(msgidlist);
                    }


                }

            }


        } else if (action.equals(MsgActionEnum.KEEPALIVE.type)) {
            /**2.4 心跳类型的消息**/
            System.out.println("收到来自Channel为[" + contchannel + "]的心跳包.....!");


        }


    }

    /**
     * 当客户端连接服务端（打开连接）
     * 获取客户端的channel 并放到ChannelGroup中进行管理
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx)
            throws Exception {
        users.add(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        /**发生异常之后关闭连接(关闭channel) 随后从channelGroup移除*/
        cause.printStackTrace();
        ctx.channel().close();
        users.remove(ctx.channel());
    }

    /**
     * 当触发handlerRemoved， ChannelGroup会自动移除客户端对应的Channel
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx)
            throws Exception {
        String channelId = ctx.channel().id().asLongText();
        System.out.println("客服端被移除" + channelId);
        users.remove(ctx.channel());
    }

}
