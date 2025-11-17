package org.example.persion;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // 你想要设置的明文密码，例如 "123456"
        String rawPassword = "Cjm1557281";
        String encodedPassword = encoder.encode(rawPassword);
        // 打印出的这个字符串就是你可以插入数据库的密码
        System.out.println(encodedPassword);
    }
}
