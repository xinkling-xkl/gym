package org.example.login.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "main-server")
public interface MainServerClient {

    @GetMapping("/api/admin/{account}")
    Map<String, Object> getAdmin(@PathVariable("account") Integer account);

    @GetMapping("/api/member/{account}")
    Map<String, Object> getMember(@PathVariable("account") Integer account);
}
