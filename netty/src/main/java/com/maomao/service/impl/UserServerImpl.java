package com.maomao.service.impl;

import com.maomao.enums.MsgActionEnum;
import com.maomao.enums.MsgSignFlagEnum;
import com.maomao.enums.SearchFriendsStatusEnum;
import com.maomao.mapper.*;
import com.maomao.netty.ChatMsg;
import com.maomao.netty.Datecontent;
import com.maomao.netty.UserChannelRel;
import com.maomao.pojo.FriendsRequset;
import com.maomao.pojo.MyFriends;
import com.maomao.pojo.Users;
import com.maomao.pojo.vo.FriendRequestVO;
import com.maomao.pojo.vo.MyFriendsVO;
import com.maomao.service.UserService;
import com.maomao.utils.FastDFSClient;
import com.maomao.utils.FileUtils;
import com.maomao.utils.JsonUtils;
import com.maomao.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class UserServerImpl implements UserService {

    @Value("${web.upload-path}")
    private String webPath;
    /**
     *
     */
    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private Sid sid;
    @Autowired
    private MyFriendsMapper myFriendsMapper;
    @Autowired
    private FriendsRequsetMapper RequsetMapper;
    @Autowired
    private UsersMapperCustom custom;
    @Autowired
    private ChatMsgMapper chatMsgMapper;
    @Autowired
    private QRCodeUtils qrCodeUtils;
    @Autowired
    private FastDFSClient fastDFSClient;


    @Override
    public boolean queryUsernameIsExist(String username) {
        Users users = new Users();
        users.setUsername(username);
        Users result = usersMapper.selectOne(users);
        return result != null ? true : false;
    }

    @Override
    public Users queryUserForLogin(String username, String pwd) {
        Example userExample = new Example(Users.class);
        Example.Criteria criteria = userExample.createCriteria();
        criteria.andEqualTo("username", username);
        criteria.andEqualTo("password", pwd);
        Users result = usersMapper.selectOneByExample(userExample);
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users user) {
        String userId = sid.nextShort();
        /**
         * 为每一个用户生成一个二维码*/


        // 为每个用户生成一个唯一的二维码
        String url = "upload/" + userId + "qrcode.png";
        String qrCodePath = webPath + url;
        // muxin_qrcode:[username]
        qrCodeUtils.createQRCode(qrCodePath, "muxin_qrcode:" + user.getUsername());
//        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);
//
//        String qrCodeUrl = "";
//        try {
//            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        user.setQrcode(url);

        user.setId(userId);
        usersMapper.insert(user);

        return user;

    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {
        usersMapper.updateByPrimaryKeySelective(user);
        return queryUserById(user.getId());

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    Users queryUserById(String userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }

    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {
        // 1搜索的用户如果不存在
        Users users = queryUserInfoByUsername(friendUsername);
        if (users == null) {
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        //2.搜索账号是你自己
        if (users.getId().equals(myUserId)) {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        //32.搜索账号是你自己
        Example fue = new Example(MyFriends.class);
        Example.Criteria fuc = fue.createCriteria();
        fuc.andEqualTo("myUserId", myUserId);
        fuc.andEqualTo("myFriendUserId", users.getId());

        MyFriends myFriendsrel = myFriendsMapper.selectOneByExample(fue);
        if (myFriendsrel != null) {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }

        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String username) {

        Example ue = new Example(Users.class);
        Example.Criteria uc = ue.createCriteria();
        uc.andEqualTo("username", username);
        return usersMapper.selectOneByExample(ue);
    }

    /**
     * *根据用户名把朋友信息查出来*
     */
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {

        Users friend = queryUserInfoByUsername(friendUsername);

        // 1.查询好友发送好友记录表/
        Example fue = new Example(FriendsRequset.class);
        Example.Criteria fuc = fue.createCriteria();
        fuc.andEqualTo("sendUserId", myUserId);
        fuc.andEqualTo("acceptUserId", friend.getId());
        FriendsRequset friendrequset = RequsetMapper.selectOneByExample(fue);
        if (friendrequset == null) {
            //2.如果不是你的好友，并且好友记录没有添加，则新增好友请求记录
            String requsetId = sid.nextShort();
            FriendsRequset requset = new FriendsRequset();
            //设置唯一ID
            requset.setId(requsetId);
            //设置发送者ID
            requset.setSendUserId(myUserId);
            //设置朋友Id
            requset.setAcceptUserId(friend.getId());
            //设置时间Id
            requset.setRequsetDateTime(new Date());
            //新增一条数据
            RequsetMapper.insert(requset);

        }

    }

    /**
     * 查询好友请求
     */
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {

        return custom.queryFriendRequestList(acceptUserId);
    }

    /**
     * 删除好友，忽略好好友
     **/
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example fue = new Example(FriendsRequset.class);
        Example.Criteria fuc = fue.createCriteria();
        fuc.andEqualTo("sendUserId", sendUserId);
        fuc.andEqualTo("acceptUserId", acceptUserId);
        RequsetMapper.deleteByExample(fue);

    }

    /**
     * 通过好友请求
     **/

    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId, acceptUserId);
        saveFriends(acceptUserId, sendUserId);
        deleteFriendRequest(sendUserId, acceptUserId);
        /***使用webscoke主动推送消息到请求发送者，更新他的通讯录为最新**/

        Channel sendChannel = UserChannelRel.get(sendUserId);
        if (sendChannel != null) {
            Datecontent dateContent = new Datecontent();
            dateContent.setAction(MsgActionEnum.PULL_FRIEND.type);
            sendChannel.writeAndFlush
                    (new TextWebSocketFrame(JsonUtils.objectToJson(dateContent)));
        }

    }

    /**
     * 保存方法
     **/
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveFriends(String sendUserId, String acceptUserId) {
        MyFriends myFriends = new MyFriends();
        String recodId = sid.nextShort();
        myFriends.setId(recodId);
        myFriends.setMyUserId(sendUserId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriendsMapper.insert(myFriends);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFriends = custom.queryMyFriends(userId);
        return myFriends;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {
        com.maomao.pojo.ChatMsg msg = new com.maomao.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msg.setId(msgId);
        msg.setAcceptUserId(chatMsg.getReceiverId());
        msg.setSendUserId(chatMsg.getSenderId());
        msg.setCreateTime(new Date());
        msg.setSignFlag(MsgSignFlagEnum.SIGEnd.type);
        msg.setMsg(chatMsg.getMsg());
        chatMsgMapper.insert(msg);
        return msgId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        custom.batchUpdateMsgSigned(msgIdList);

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.maomao.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {
        Example chatExample = new Example(com.maomao.pojo.ChatMsg.class);
        Example.Criteria chatExampleCriteria = chatExample.createCriteria();
        chatExampleCriteria.andEqualTo("signFlag", 0);
        chatExampleCriteria.andEqualTo("acceptUserId", acceptUserId);
        List<com.maomao.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);

        return result;
    }
}
