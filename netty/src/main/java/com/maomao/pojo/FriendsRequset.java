package com.maomao.pojo;

import java.util.Date;
import javax.persistence.*;
//好友请求
@Table(name = "friends_requset")
public class FriendsRequset {
    @Id
    private String id;

    @Column(name = "send_user_id")
    private String sendUserId;

    @Column(name = "accept_user_id")
    private String acceptUserId;

    @Column(name = "requset_date_time")
    private Date requsetDateTime;

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return send_user_id
     */
    public String getSendUserId() {
        return sendUserId;
    }

    /**
     * @param sendUserId
     */
    public void setSendUserId(String sendUserId) {
        this.sendUserId = sendUserId;
    }

    /**
     * @return accept_user_id
     */
    public String getAcceptUserId() {
        return acceptUserId;
    }

    /**
     * @param acceptUserId
     */
    public void setAcceptUserId(String acceptUserId) {
        this.acceptUserId = acceptUserId;
    }

    /**
     * @return requset_date_time
     */
    public Date getRequsetDateTime() {
        return requsetDateTime;
    }

    /**
     * @param requsetDateTime
     */
    public void setRequsetDateTime(Date requsetDateTime) {
        this.requsetDateTime = requsetDateTime;
    }
}