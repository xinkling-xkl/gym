package com.gym.common;

/**
 * 线程级用户上下文 — 用于 AI 权限隔离
 */
public class UserContext {
    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    private UserContext() {}

    public static void set(UserInfo userInfo) {
        CONTEXT.set(userInfo);
    }

    public static UserInfo get() {
        return CONTEXT.get();
    }

    public static Integer getAccount() {
        UserInfo u = CONTEXT.get();
        return u != null ? u.getAccount() : null;
    }

    public static String getRole() {
        UserInfo u = CONTEXT.get();
        return u != null ? u.getRole() : null;
    }

    public static String getName() {
        UserInfo u = CONTEXT.get();
        return u != null ? u.getName() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static class UserInfo {
        private final Integer account;
        private final String name;
        private final String role;

        public UserInfo(Integer account, String name, String role) {
            this.account = account;
            this.name = name;
            this.role = role;
        }

        public Integer getAccount() { return account; }
        public String getName() { return name; }
        public String getRole() { return role; }
    }
}
