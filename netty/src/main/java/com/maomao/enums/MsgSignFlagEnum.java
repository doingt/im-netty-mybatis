package com.maomao.enums;

/**
 * 消息签收状态
 **/
public enum MsgSignFlagEnum {
    UNSIGN(0, "签收"),
    SIGEnd(1, "未签收");
    public final Integer type;
    public final String content;

    MsgSignFlagEnum(Integer type, String content) {
        this.type = type;
        this.content = content;
    }

    public Integer getType() {
        return type;
    }
}
