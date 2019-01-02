package com.maomao.utils;

import com.maomao.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;


@SpringBootTest
@RunWith(SpringRunner.class)
public class QRCodeUtilsTest {


    @Value("${web.upload-path}")
    private String webPath;

    @Test
    public void createQRCode() {
        System.out.println(webPath);
//
//        String filePath="D:\\MyWord\\Desktop\\coding-261\\后端\\imooc-muxin-mybatis\\netty\\src\\main\\resources\\static\\abacqrcode.png";
//        String content="muxin_qrcode:adfasfde";
//        QRCodeUtils utils=new QRCodeUtils();
//        utils.createQRCode(filePath,content);
    }
}