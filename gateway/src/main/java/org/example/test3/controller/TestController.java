package org.example.test3.controller;

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

    /**
     * 本地接口：返回 time-server 的信息，供其他服务调用
     */
    @GetMapping("/info")
    public String info() {
        return "Hello from time-server (port 8086)";
    }

    /**
     * 测试1：通过 RestTemplate 调用 order-server 的接口
     */
    @GetMapping("/callOrder")
    public String callOrderServer() {
        String url = "http://order-server/test/info";
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 测试2：通过 RestTemplate 调用 stock-server 的接口
     */
    @GetMapping("/callStock")
    public String callStockServer() {
        String url = "http://stock-server/test/info";
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 测试3：链式调用 - 依次调用 order-server 和 stock-server
     */
    @GetMapping("/chainCall")
    public String chainCall() {
        String orderUrl = "http://order-server/test/info";
        String stockUrl = "http://stock-server/test/info";

        String orderResult = restTemplate.getForObject(orderUrl, String.class);
        String stockResult = restTemplate.getForObject(stockUrl, String.class);

        return "time-server -> order-server: " + orderResult + " | time-server -> stock-server: " + stockResult;
    }
}
