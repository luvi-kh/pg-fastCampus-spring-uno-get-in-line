package com.uno.getinline.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BaseController  {

    @GetMapping("/")
    public String root() throws Exception{
        throw new Exception("테스트");
//        return "index";
    }
}
