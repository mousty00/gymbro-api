package com.mousty.gymbro.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Service
public class S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final S3Client s3;
    private final S3Presigner s3Presigner;

    public S3Service(S3Client s3, S3Presigner s3Presigner) {
        this.s3 = s3;
        this.s3Presigner = s3Presigner;
    }


    /**
     * Uploads a file to S3 bucket with a specific file name
     * @param file The multipart file to upload
     * @param fileName The desired file name in S3
     * @return The key/filename of the uploaded file (not the full URL)
     */
    public String uploadFile(MultipartFile file, String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3.putObject(objectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));

            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * Generates a pre-signed URL for accessing a file
     * @param fileKey The S3 key/filename
     * @param expiration Duration for which the URL should be valid
     * @return Pre-signed URL
     */
    public String generatePresignedUrl(String fileKey, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to generate pre-signed URL", e);
        }
    }

    /**
     * Generates a pre-signed URL with default 1 hour expiration
     * @param fileKey The S3 key/filename
     * @return Pre-signed URL valid for 1 hour
     */
    public String generatePresignedUrl(String fileKey) {
        return generatePresignedUrl(fileKey, Duration.ofHours(1));
    }

    /**
     * Downloads a file from S3
     * @param fileKey The S3 key of the file
     * @return The file as a byte array
     */
    public byte[] downloadFile(String fileKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            ResponseBytes<GetObjectResponse> res = s3.getObjectAsBytes(getObjectRequest);
            return res.asByteArray();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    /**
     * Deletes a file from S3
     * @param fileKey The S3 key of the file to delete
     */
    public void deleteFile(String fileKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    /**
     * Checks if a file exists in S3
     * @param fileKey The S3 key of the file to check
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String fileKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3.headObject(headObjectRequest);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new RuntimeException("Failed to check file existence in S3", e);
        }
    }

    /**
     * Gets the content type of a file in S3
     * @param fileKey The S3 key of the file
     * @return The content type
     */
    public String getFileContentType(String fileKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            HeadObjectResponse response = s3.headObject(headObjectRequest);
            return response.contentType();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to get file content type from S3", e);
        }
    }
}