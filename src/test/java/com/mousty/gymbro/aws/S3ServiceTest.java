package com.mousty.gymbro.aws;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock private S3Client s3;
    @Mock private S3Presigner presigner;
    @InjectMocks private S3Service s3Service;

    @Test
    @DisplayName("no image key (text-only post, user without picture) gives no URL instead of an SDK error")
    void nullOrBlankKey() {
        assertThat(s3Service.generatePresignedUrl(null)).isNull();
        assertThat(s3Service.generatePresignedUrl("  ")).isNull();
        verifyNoInteractions(presigner);
    }
}
