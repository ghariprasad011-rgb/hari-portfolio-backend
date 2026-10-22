
package com.hari.portfoliobackend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String newPassword = "Hari@2106";
        String hashedPassword = encoder.encode(newPassword);

        System.out.println("PASSWORD_HASH=" + hashedPassword);
    }
}
