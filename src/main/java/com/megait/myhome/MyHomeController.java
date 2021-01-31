package com.megait.myhome;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MyHomeController {


    @RequestMapping("/")
    public String index(){
        return "view/index";
    }

}
