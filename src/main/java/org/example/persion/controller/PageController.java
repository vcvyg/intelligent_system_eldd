package org.example.persion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面跳转控制器
 */
@Controller
public class PageController {

    /**
     * 根路径跳转到欢迎页
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/welcome.html";
    }
}
