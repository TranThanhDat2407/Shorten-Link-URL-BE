package com.example.short_link;

import com.example.short_link.util.GeoInfo;
import com.example.short_link.util.IpGeolocationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class IpGeolocationUtilTest {
    private IpGeolocationUtil ipGeolocationUtil;

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng (cần mô phỏng @PostConstruct)
        ipGeolocationUtil = new IpGeolocationUtil();
        ipGeolocationUtil.init(); // Gọi thủ công init để load DB
    }

    @Test
    void testLookupGoogleDns() {
        // IP Google Public DNS - Nên ra United States
        String ipAddress = "8.8.8.8";
        GeoInfo geoInfo = ipGeolocationUtil.lookup(ipAddress);

        assertEquals("United States", geoInfo.getCountry());
        // Có thể kiểm tra city, nhưng nó có thể thay đổi tùy thuộc vào phiên bản GeoLite2-City
        System.out.println("8.8.8.8: " + geoInfo.getCountry() );
    }

    @Test
    void testLookupLocalhost() {
        // IP Localhost
        String ipAddress = "127.0.0.1";
        GeoInfo geoInfo = ipGeolocationUtil.lookup(ipAddress);

        assertEquals("Localhost", geoInfo.getCountry());
    }

    @Test
    void testLookupVnptDns() {
        // IP của VNPT (một ví dụ)
        String ipAddress = "203.162.4.190"; // IP VNPT
        GeoInfo geoInfo = ipGeolocationUtil.lookup(ipAddress);

        // Kiểm tra với một IP ở Việt Nam
        assertEquals("Vietnam", geoInfo.getCountry());
        System.out.println("203.162.4.190: " + geoInfo.getCountry() );
    }

    @Test
    void testLookupCloudflareDns() {
        String ipAddress = "1.1.1.1";
        GeoInfo geoInfo = ipGeolocationUtil.lookup(ipAddress);

        System.out.println("1.1.1.1: " + geoInfo.getCountry() );

        // Nếu  vẫn là "Unknown", thì hãy làm Bước 2.
        assertNotEquals("Unknown",  "IP 1.1.1.1 nên trả về một thành phố cụ thể.");
    }
}
