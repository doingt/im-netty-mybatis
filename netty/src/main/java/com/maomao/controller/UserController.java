package com.maomao.controller;

import ch.qos.logback.core.util.FileUtil;
import com.maomao.enums.OperatorFriendRequestTypeEnum;
import com.maomao.enums.SearchFriendsStatusEnum;
import com.maomao.pojo.ChatMsg;
import com.maomao.pojo.Users;
import com.maomao.pojo.bo.UsersBO;
import com.maomao.pojo.vo.MyFriendsVO;
import com.maomao.pojo.vo.UsersVo;
import com.maomao.service.UserService;
import com.maomao.utils.FastDFSClient;
import com.maomao.utils.FileUtils;
import com.maomao.utils.IMoocJSONResult;
import com.maomao.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("u")
public class UserController {
    @Autowired
    UserService userService;

    @Autowired
    FastDFSClient fastDFSClient;
    @Value("${web.upload-path}")
    private String webPath;

    @PostMapping("login")
    public IMoocJSONResult Login(@RequestBody Users users) throws Exception {
        /**
         * *0.判断用户名或者密码不能为空*/
        if (StringUtils.isBlank(users.getUsername())
                || StringUtils.isBlank(users.getPassword())) {
            return IMoocJSONResult.errorMsg("用户名或密码不能为空...");
        }
        /**
         * 1.判断用户名是否存在,如果存在就登录，如果不存在就注册*/
        boolean is = userService.queryUsernameIsExist(users.getUsername());
        Users userResult = null;

        if (is
        ) {
            //登录
            userResult = userService.queryUserForLogin(users.getUsername(),
                    MD5Utils.getMD5Str(users.getPassword()));
            if (userResult == null) {
                return IMoocJSONResult.errorMsg("用户名或密码错误...");
            }
        } else {
            //注册
            users.setNickname(users.getUsername());
            users.setPassword(MD5Utils.getMD5Str(users.getPassword()));
            users.setFaceImage("");
            users.setFaceImageBig("");
            userResult = userService.saveUser(users);
        }
        UsersVo usersVo = new UsersVo();
        BeanUtils.copyProperties(userResult, usersVo);
        return IMoocJSONResult.ok(usersVo);

    }

    /**
     * 上传用户头像
     */
    @PostMapping("base64")
    public IMoocJSONResult uploadFaceBase64( @RequestBody UsersBO userBO)
            throws Exception {
        /**获取前端传过来的base64位字符串，然后转换为文件对象在上传**/
        String base64Date = userBO.getFaceData();


        /**写入一个临时路径**/
        String url = "upload/" + UUID.randomUUID() + "face64.png";
        String userFacepath = webPath + url;
        System.out.println("file:"+userFacepath);
        //上传文件到fastdfs
        FileUtils.base64ToFile(userFacepath, base64Date);
        /*MultipartFile facefile = FileUtils.fileToMultipart(userFacepath);
        String url = fastDFSClient.uploadBase64(facefile);
        System.out.println(url);
        *//**获取缩量图的url**//*
        String tump = "80*80.";
        String arr[] = url.split("\\.");
        String tumparrurl = arr[0] + tump + arr[1];
        */


        /**跟新用户头像**/
        Users users = new Users();
        users.setId(userBO.getUserId());
        users.setFaceImage(url);
        users.setFaceImageBig(url);

        users = userService.updateUserInfo(users);


        UsersVo usersVo = new UsersVo();
        BeanUtils.copyProperties(users, usersVo);
        return IMoocJSONResult.ok(usersVo);
    }

    @PostMapping("/setNickname")
    public IMoocJSONResult setNickname(@RequestBody UsersBO userBO) throws Exception {

        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());

        Users result = userService.updateUserInfo(user);

        return IMoocJSONResult.ok(result);
    }

    /***
     * 添加朋友
     * 前置条件 -1.搜索的用户如果不存在，返回（无此用户）
     * 前置条件 -2.搜索账号是你自己，返回（不能添加自己）
     * 前置条件 -3.搜索的好友已经是你的好友，返回（该用户已经是你好友）
     */
    @PostMapping("/search")
    public IMoocJSONResult searchUse(String myUserId, String friendUsername) {
        //用户名不能为空
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("");
        }
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status.equals(SearchFriendsStatusEnum.SUCCESS.status)) {
            Users friend = userService.queryUserInfoByUsername(friendUsername);
            UsersVo usersVo = new UsersVo();
            BeanUtils.copyProperties(friend, usersVo);
            return IMoocJSONResult.ok(usersVo);
        } else {
            String error = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(error);
        }

    }

    /***
     * 发送添加好友的请求
     * 前置条件 -1.搜索的用户如果不存在，返回（无此用户）
     * 前置条件 -2.搜索账号是你自己，返回（不能添加自己）
     * 前置条件 -3.搜索的好友已经是你的好友，返回（该用户已经是你好友）
     */
    @PostMapping("/addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myUserId, String friendUsername) {
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("");
        }
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status.equals(SearchFriendsStatusEnum.SUCCESS.status)) {
            userService.sendFriendRequest(myUserId, friendUsername);
        } else {
            String error = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(error);
        }

        return IMoocJSONResult.ok();
    }

    /***查询好友申请*/
    @PostMapping("/queryFriendRequests")
    public IMoocJSONResult queryFriendRequests(String userId) {
        //0 判断不能为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }
        //1 查询用户接受到的朋友申请
        return IMoocJSONResult.ok(userService.queryFriendRequestList(userId));
    }

    /***好友申请
     * 通过或者忽略*/
    @PostMapping("/operFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId,
                                             String sendUserId, Integer operType) {
        //0  acceptUserId,sendUserId operType判断不能为空
        if (StringUtils.isBlank(acceptUserId)
                || StringUtils.isBlank(sendUserId)
                || operType == null) {
            return IMoocJSONResult.errorMsg("");
        }

        //1如果operType,没有对应的枚举值，就抛出空错误信息；
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return IMoocJSONResult.errorMsg("");
        }
        //2 如果忽略好友请求，则直接删除好友请求数据库记路；
        if (operType.equals(OperatorFriendRequestTypeEnum.IGNORE.type)) {
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        }
        //3 如果同意好友请求，则互相增加好友记录到数据库对应的表。
        // 然后删除好友请求的数据库对应的记录
        else if (operType.equals(OperatorFriendRequestTypeEnum.PASS.type)) {
            userService.passFriendRequest(sendUserId, acceptUserId);
        }
        return IMoocJSONResult.ok();

    }

    /**
     * 查询我的好友列表
     **/
    @PostMapping("/myFriends")
    public IMoocJSONResult myFriends(String userId) {
        //0.判断userIs不能为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }
        //1. 数据库查询好友列表
        List<MyFriendsVO> myFriendsVOS = userService.queryMyFriends(userId);
        return IMoocJSONResult.ok(myFriendsVOS);
    }

    /**
     * 用户手机端获取未读消息列表
     ***/
    @PostMapping("/getUnReadMsgList")
    public IMoocJSONResult getUnReadMsgList(String acceptUserId) {
        //判断acceptUserId 不能为空
        if (StringUtils.isBlank(acceptUserId)) {
            return IMoocJSONResult.errorMsg("");
        }


        List<ChatMsg> unrendMsgList = userService.getUnReadMsgList(acceptUserId);
        return IMoocJSONResult.ok(unrendMsgList);
    }
}
