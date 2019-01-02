package com.maomao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
// 扫描mybatis mapper包路径
@MapperScan(basePackages = {"com.maomao.mapper"})
@ComponentScan(basePackages = {"org.n3r.idworker", "com.maomao"})
public class Application {

    @Bean
    public SpringUtil getSpringUtil() {
        return new SpringUtil();
    }

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }
}
