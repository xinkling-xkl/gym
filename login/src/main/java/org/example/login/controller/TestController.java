package org.example.login.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/info")
    public String info() {
        return "Hello from login-server (port 8085)";
    }

    @GetMapping("/callTime")
    public String callTimeServer() {
        String url = "http://time-server/test/info";
        return restTemplate.getForObject(url, String.class);
    }

    @GetMapping("/callOrder")
    public String callOrderServer() {
        String url = "http://order-server/test/info";
        return restTemplate.getForObject(url, String.class);
    }

    @GetMapping("/chainCall")
    public String chainCall() {
        String orderUrl = "http://order-server/test/info";
        String timeUrl = "http://time-server/test/info";

        String orderResult = restTemplate.getForObject(orderUrl, String.class);
        String timeResult = restTemplate.getForObject(timeUrl, String.class);

        return "login-server -> order-server: " + orderResult + " | login-server -> time-server: " + timeResult;
    }
}
