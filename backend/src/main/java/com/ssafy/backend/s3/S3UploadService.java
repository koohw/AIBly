package com.ssafy.ABLRI.infra.s3;

import com.ssafy.ABLRI.config.S3Config;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Config s3Config;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}") // region 직접 주입
    private String region;

    public String saveFile(MultipartFile multipartFile) throws IOException {
        String originalFilename = multipartFile.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID() + "_" + originalFilename;

        S3Client s3Client = s3Config.getS3Client();  // 주입된 Config에서 가져옴

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uniqueFileName)
                .contentType(multipartFile.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                        multipartFile.getInputStream(),
                        multipartFile.getSize()
                ));

        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + uniqueFileName;
    }
}