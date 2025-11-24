package com.example.short_link.util;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

public class JwtSecretKeyGenerator {
    public static void main(String[] args) {
        // Tạo key cực mạnh cho HS512 (yêu cầu >= 256 bit, mình tạo 512 bit cho an toàn tuyệt đối)
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);

        // In ra dạng Base64 – copy cái này dán vào application.yml
        String secretKey = Encoders.BASE64.encode(key.getEncoded());

        System.out.println("=== YOUR SUPER SECURE JWT SECRET KEY ===");
        System.out.println(secretKey);
        System.out.println("==========================================");
        System.out.println("Độ dài: " + secretKey.length() + " ký tự");
        System.out.println("→ Dán vào application.yml: jwt.secretKey = " + secretKey);
    }
}
