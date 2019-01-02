package com.maomao.mapper;

import com.maomao.pojo.Users;
import com.maomao.pojo.vo.FriendRequestVO;
import com.maomao.pojo.vo.MyFriendsVO;
import com.maomao.utils.MyMapper;

import java.util.List;

public interface UsersMapperCustom extends MyMapper<Users> {
    List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    List<MyFriendsVO> queryMyFriends(String userId);

    void batchUpdateMsgSigned(List<String> msgIdList);

}
