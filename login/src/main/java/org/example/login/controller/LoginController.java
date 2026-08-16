package org.example.login.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.login.client.MainServerClient;
import org.example.login.common.Result;
import org.example.login.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/login")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private MainServerClient mainServerClient;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping
    @SentinelResource(value = "login", blockHandler = "handleBlock")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> loginData) {
        Object accountObj = loginData.get("account");
        Integer account;
        if (accountObj instanceof Number) {
            account = ((Number) accountObj).intValue();
        } else {
            account = Integer.parseInt(accountObj.toString());
        }
        String password = (String) loginData.get("password");

        // 统一查询：main-server 自动判断 admin/member/employee
        Map<String, Object> userResult = mainServerClient.getUserByAccount(account);

        if (userResult == null || (Integer) userResult.get("code") != 200) {
            return Result.error(404, "账号不存在");
        }

        Map<String, Object> userData = (Map<String, Object>) userResult.get("data");
        String role = (String) userData.get("role");
        String name = (String) userData.get("name");
        String storedPassword = (String) userData.get("password");
        Object staffObj = userData.get("staff");
        String staff = staffObj != null ? staffObj.toString() : "";

        if (!checkPassword(password, storedPassword)) {
            return Result.error(401, "密码错误");
        }

        String token = JwtUtil.generateToken(account, name, role);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", role);
        data.put("name", name);
        data.put("account", account);
        if (!staff.isEmpty()) {
            data.put("staff", staff);
        }
        return Result.success(role + "登录成功", data);
    }

    private boolean checkPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    @PostMapping("/validate")
    @SentinelResource(value = "login-validate", blockHandler = "handleBlock")
    public Result<Map<String, Object>> validateToken(@RequestBody Map<String, String> tokenData) {
        String token = tokenData.get("token");
        if (JwtUtil.validateToken(token)) {
            Integer account = JwtUtil.getAccountFromToken(token);
            String name = JwtUtil.getNameFromToken(token);
            String role = JwtUtil.getRoleFromToken(token);

            Map<String, Object> data = Map.of(
                    "account", (Object) account,
                    "name", name,
                    "role", role
            );
            return Result.success("token有效", data);
        }
        return Result.error(401, "token无效或已过期");
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
