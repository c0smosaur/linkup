package com.core.linkup.common.utils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class S3Utils {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;


    public void saveFile(MultipartFile file, String filename) throws IOException {

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(bucket, filename, file.getInputStream(), metadata);
    }

    public void deleteFile(String filename)  {
        amazonS3.deleteObject(bucket, filename);
    }

    public String getFileUrl(String filename) {
        return amazonS3.getUrl(bucket, filename).toString();
    }

}
