package org.example.login.controller;

import org.example.login.client.MainServerClient;
import org.example.login.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/login")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private MainServerClient mainServerClient;

    @PostMapping
    public Map<String, Object> login(@RequestBody Map<String, Object> loginData) {
        Map<String, Object> result = new HashMap<>();
        try {
            Object accountObj = loginData.get("account");
            Integer account;
            if (accountObj instanceof Number) {
                account = ((Number) accountObj).intValue();
            } else {
                account = Integer.parseInt(accountObj.toString());
            }
            String password = (String) loginData.get("password");

            // 先查询管理员表
            Map<String, Object> adminResult = mainServerClient.getAdmin(account);

            if (adminResult != null && (Integer) adminResult.get("code") == 200) {
                Map<String, Object> adminData = (Map<String, Object>) adminResult.get("data");
                String storedPassword = (String) adminData.get("adminPassword");

                if (password.equals(storedPassword)) {
                    String token = JwtUtil.generateToken(account, "管理员", "ADMIN");
                    result.put("code", 200);
                    result.put("token", token);
                    result.put("role", "ADMIN");
                    result.put("name", "管理员");
                    result.put("account", account);
                    result.put("message", "管理员登录成功");
                    return result;
                } else {
                    result.put("code", 401);
                    result.put("message", "密码错误");
                    return result;
                }
            }

            // 再查询会员表
            Map<String, Object> memberResult = mainServerClient.getMember(account);

            if (memberResult != null && (Integer) memberResult.get("code") == 200) {
                Map<String, Object> memberData = (Map<String, Object>) memberResult.get("data");
                String storedPassword = (String) memberData.get("memberPassword");
                String memberName = (String) memberData.get("memberName");

                if (password.equals(storedPassword)) {
                    String token = JwtUtil.generateToken(account, memberName, "MEMBER");
                    result.put("code", 200);
                    result.put("token", token);
                    result.put("role", "MEMBER");
                    result.put("name", memberName);
                    result.put("account", account);
                    result.put("message", "会员登录成功");
                    return result;
                } else {
                    result.put("code", 401);
                    result.put("message", "密码错误");
                    return result;
                }
            }

            // 两个表都不存在
            result.put("code", 404);
            result.put("message", "账号不存在");

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "登录失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/validate")
    public Map<String, Object> validateToken(@RequestBody Map<String, String> tokenData) {
        Map<String, Object> result = new HashMap<>();
        try {
            String token = tokenData.get("token");
            if (JwtUtil.validateToken(token)) {
                Integer account = JwtUtil.getAccountFromToken(token);
                String name = JwtUtil.getNameFromToken(token);
                String role = JwtUtil.getRoleFromToken(token);

                result.put("code", 200);
                result.put("account", account);
                result.put("name", name);
                result.put("role", role);
                result.put("message", "token有效");
            } else {
                result.put("code", 401);
                result.put("message", "token无效或已过期");
            }
        } catch (Exception e) {
            result.put("code", 401);
            result.put("message", "token无效");
        }
        return result;
    }
}
