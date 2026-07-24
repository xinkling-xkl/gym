package org.example.aichat.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "ai-python", url = "${ai.python.url}")
public interface AiPythonClient {

    @PostMapping("/chat")
    Map<String, Object> chat(@RequestBody Map<String, Object> request);
}
