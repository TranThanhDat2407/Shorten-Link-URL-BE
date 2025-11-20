package com.example.short_link.util;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

@Component
public class IpGeolocationUtil {
    private DatabaseReader dbReader;
    private static final String GEOIP_DB_FILE = "GeoLite2-City.mmdb";

    @PostConstruct
    public void init() {
        try {
            // Lấy InputStream từ file trong thư mục resources
            InputStream database = getClass().getClassLoader().getResourceAsStream(GEOIP_DB_FILE);
            if (database == null) {
                // Xử lý trường hợp không tìm thấy file DB
                System.err.println("GeoLite2 database file not found in resources: " + GEOIP_DB_FILE);
                return;
            }
            this.dbReader = new DatabaseReader.Builder(database).build();
        } catch (IOException e) {
            System.err.println("Could not initialize MaxMind GeoLite2 DatabaseReader: " + e.getMessage());
        }
    }

    public GeoInfo lookup(String ipAddress) {
        // Xử lý IP Localhost (IPv4 và IPv6)
        if (ipAddress == null ||
                ipAddress.equals("127.0.0.1") ||
                ipAddress.equals("0:0:0:0:0:0:1") ||
                ipAddress.equals("0:0:0:0:0:0:0:1")) { // Thêm full IPv6 loopback
            return GeoInfo.builder().country("Localhost").build();
        }


        if (dbReader == null) {
            // Trường hợp DB chưa khởi tạo (do lỗi hoặc thiếu file)
            return GeoInfo.builder().country("Unknown").build();
        }

        try {
            InetAddress ip = InetAddress.getByName(ipAddress);
            CityResponse response = dbReader.city(ip);

            String country = response.getCountry().getName();
            String city = response.getCity().getName();

            // Trả về dữ liệu đã tra cứu
            return GeoInfo.builder()
                    .country(country != null ? country : "Unknown")
                    .build();

        } catch (IOException | GeoIp2Exception e) {
            // Bắt các ngoại lệ liên quan đến IP không hợp lệ hoặc không tìm thấy
            // logger.warning("Geolocation lookup failed for IP " + ipAddress + ": " + e.getMessage());
            return GeoInfo.builder().country("Unknown").build();
        }
    }

}
