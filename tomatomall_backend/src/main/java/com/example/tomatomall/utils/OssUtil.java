package com.example.tomatomall.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

@Component
@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties("aliyun.oss")
public class OssUtil {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    public String upload(String objectName, InputStream inputStream) {
        if (endpoint == null || accessKeyId == null || accessKeySecret == null || bucketName == null) {
            throw new IllegalStateException("aliyun.oss 配置未设置，请在 application.yml 中配置 endpoint/accessKeyId/accessKeySecret/bucketName");
        }

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
        String url;
        try {
            ossClient.putObject(putObjectRequest);
            // 生成带过期时间的可访问 URL（这里设置为 1 天）
            Date expiration = new Date(System.currentTimeMillis() + 24L * 3600 * 1000);
            url = ossClient.generatePresignedUrl(bucketName, objectName, expiration).toString().split("\\?Expires")[0];
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return url;
    }
}