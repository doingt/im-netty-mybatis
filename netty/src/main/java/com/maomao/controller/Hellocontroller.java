package com.maomao.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Hellocontroller {
    @GetMapping("/hello")
    private String hello() {
        return "hello maomao~~~";
    }
}
