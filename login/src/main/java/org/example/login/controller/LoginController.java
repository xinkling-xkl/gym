package org.example.login.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.login.client.MainServerClient;
import org.example.login.common.Result;
import org.example.login.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

        Map<String, Object> adminResult = mainServerClient.getAdmin(account);

        if (adminResult != null && (Integer) adminResult.get("code") == 200) {
            Map<String, Object> adminData = (Map<String, Object>) adminResult.get("data");
            String storedPassword = (String) adminData.get("adminPassword");

            if (checkPassword(password, storedPassword)) {
                String token = JwtUtil.generateToken(account, "管理员", "ADMIN");
                Map<String, Object> data = Map.of(
                        "token", token,
                        "role", "ADMIN",
                        "name", "管理员",
                        "account", account
                );
                return Result.success("管理员登录成功", data);
            }
            return Result.error(401, "密码错误");
        }

        Map<String, Object> memberResult = mainServerClient.getMember(account);

        if (memberResult != null && (Integer) memberResult.get("code") == 200) {
            Map<String, Object> memberData = (Map<String, Object>) memberResult.get("data");
            String storedPassword = (String) memberData.get("memberPassword");
            String memberName = (String) memberData.get("memberName");

            if (checkPassword(password, storedPassword)) {
                String token = JwtUtil.generateToken(account, memberName, "MEMBER");
                Map<String, Object> data = Map.of(
                        "token", token,
                        "role", "MEMBER",
                        "name", memberName,
                        "account", account
                );
                return Result.success("会员登录成功", data);
            }
            return Result.error(401, "密码错误");
        }

        // 检查员工表
        Map<String, Object> employeeResult = mainServerClient.getEmployee(account);

        if (employeeResult != null && (Integer) employeeResult.get("code") == 200) {
            Map<String, Object> employeeData = (Map<String, Object>) employeeResult.get("data");
            String storedPassword = (String) employeeData.get("employeePassword");
            String employeeName = (String) employeeData.get("employeeName");

            if (checkPassword(password, storedPassword)) {
                String token = JwtUtil.generateToken(account, employeeName, "EMPLOYEE");
                Map<String, Object> data = Map.of(
                        "token", token,
                        "role", "EMPLOYEE",
                        "name", employeeName,
                        "account", account
                );
                return Result.success("员工登录成功", data);
            }
            return Result.error(401, "密码错误");
        }

        return Result.error(404, "账号不存在");
    }

    /**
     * 密码校验：优先 BCrypt，兼容旧明文密码。
     * 若数据库密码为明文，请重新创建用户或手动更新为 BCrypt 密文。
     */
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
