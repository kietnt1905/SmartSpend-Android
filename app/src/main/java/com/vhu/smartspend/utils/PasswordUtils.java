package com.vhu.smartspend.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Hỗ trợ băm mật khẩu một chiều bằng thuật toán BCrypt (tự động tạo salt ngẫu nhiên).
 */
public class PasswordUtils {

    /**
     * Băm mật khẩu gốc thành chuỗi BCrypt để lưu vào SQLite.
     */
    public static String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * Kiểm tra chuỗi mật khẩu trong DB đã ở định dạng băm BCrypt hay chưa.
     */
    public static boolean isHashed(String storedPassword) {
        return storedPassword != null
                && (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$"));
    }

    /**
     * Đối chiếu mật khẩu người dùng nhập với mật khẩu lưu trong SQLite.
     * Hỗ trợ tương thích ngược cho tài khoản cũ chưa băm.
     */
    public static boolean verifyPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isHashed(storedPassword)) {
            try {
                return BCrypt.checkpw(rawPassword, storedPassword);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return storedPassword.equals(rawPassword);
    }
}
