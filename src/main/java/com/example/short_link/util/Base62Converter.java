package com.example.short_link.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Converter {
    public static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public String encode(long id){
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            int index = (int)(id % 62);
            sb.append(BASE62.charAt(index));
            id /= 62;
        }
        return sb.reverse().toString();
    }

    public long decode(String code){
        long id = 0;
        for (int i = 0; i < code.length(); i++) {
            id = id * 62 + BASE62.indexOf(code.charAt(i));
        }
        return id;
    }
}
