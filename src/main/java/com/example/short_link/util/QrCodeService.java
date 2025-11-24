package com.example.short_link.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.short_link.entity.Link;
import com.example.short_link.repository.LinkRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final Cloudinary cloudinary;
    private final LinkRepository linkRepository;

    @Value("${spring.application.frontend-domain}")
    private String frontEndDomain;

    private static final int QR_SIZE = 300; // pixel

    @Transactional
    public void generateAndUploadQrCode(Link link) throws Exception {
        if (link.isQrGenerated()) {
            return; // đã có rồi thì bỏ qua
        }

        String shortUrl = frontEndDomain +"/"+ link.getShortCode();
        // thay bằng domain thật của bạn

        // 1. Tạo QR code dưới dạng byte[]
        BitMatrix bitMatrix = new QRCodeWriter()
                .encode(shortUrl, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();

        // 2. Upload lên Cloudinary
        String fileName = "qr/" + link.getShortCode() + "_" + Instant.now().toEpochMilli();

        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                pngData,
                ObjectUtils.asMap(
                        "public_id", fileName,
                        "folder", "short-link-qr",
                        "overwrite", true,
                        "resource_type", "image"
                )
        );

        String qrUrl = (String) uploadResult.get("secure_url");
       //String storageKey = (String) uploadResult.get("public_id") + "." + (String) uploadResult.get("format");

        // 3. Cập nhật entity
        link.setQrCodeUrl(qrUrl);
//        link.setQrCodeStorageKey(storageKey);
        link.setQrGenerated(true);

        linkRepository.save(link);
    }
}
