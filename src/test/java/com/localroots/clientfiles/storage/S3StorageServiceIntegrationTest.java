package com.localroots.clientfiles.storage;

import com.localroots.clientfiles.attachment.AttachmentCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class S3StorageServiceIntegrationTest {

    private static final String TEST_CONTENT_TYPE = "application/pdf";

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3StorageService storageService;

    @Autowired
    private Environment environment;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String uploadedKey;

    @AfterEach
    void tearDown() {
        if (uploadedKey != null) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(storageService.bucket())
                    .key(uploadedKey)
                    .build());
        }
    }

    @Test
    void canConnectToConfiguredS3Bucket() {
        assumeS3IsConfigured();

        s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(storageService.bucket())
                .build());

        assertThat(storageService.bucket())
                .isEqualTo(environment.getProperty("storage.s3.bucket"));
    }

    @Test
    void canUploadRealFileVerifyItAndDownloadItBack() throws Exception {
        assumeS3IsConfigured();

        UUID tenantId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();

        byte[] fileBytes = """
                %PDF-1.4
                1 0 obj
                << /Type /Catalog >>
                endobj
                %%EOF
                """.getBytes();

        Path testFile = Files.createTempFile("client-files-s3-upload-", ".pdf");
        Files.write(testFile, fileBytes);

        uploadedKey = storageService.buildAttachmentKey(
                tenantId,
                contactId,
                attachmentId,
                AttachmentCategory.DOCUMENTS,
                testFile.getFileName().toString()
        );

        S3StorageService.PresignedUpload upload = storageService.presignUpload(
                uploadedKey,
                TEST_CONTENT_TYPE,
                null
        );

        HttpRequest.Builder uploadRequestBuilder = HttpRequest.newBuilder(upload.url())
                .PUT(HttpRequest.BodyPublishers.ofFile(testFile));

        upload.requiredHeaders().forEach(uploadRequestBuilder::header);

        HttpResponse<String> uploadResponse = httpClient.send(
                uploadRequestBuilder.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(uploadResponse.statusCode())
                .as("S3 presigned PUT response body: %s", uploadResponse.body())
                .isBetween(200, 299);

        S3StorageService.UploadedObject uploadedObject = storageService.headObject(
                uploadedKey,
                false
        );

        assertThat(uploadedObject.sizeBytes()).isEqualTo(fileBytes.length);
        assertThat(uploadedObject.contentType()).isEqualTo(TEST_CONTENT_TYPE);
        assertThat(uploadedObject.etag()).isNotBlank();

        S3StorageService.PresignedDownload download = storageService.presignDownload(
                uploadedKey,
                "downloaded-test-file.pdf",
                true
        );

        HttpResponse<byte[]> downloadResponse = httpClient.send(
                HttpRequest.newBuilder(download.url())
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(downloadResponse.statusCode()).isEqualTo(200);
        assertThat(downloadResponse.body()).isEqualTo(fileBytes);
    }

    @Test
    void generatedAttachmentKeyBelongsToTenant() {
        assumeS3IsConfigured();

        UUID tenantId = UUID.randomUUID();

        String key = storageService.buildAttachmentKey(
                tenantId,
                null,
                UUID.randomUUID(),
                AttachmentCategory.DOCUMENTS,
                "my unsafe file name!!.pdf"
        );

        storageService.assertKeyBelongsToTenant(key, tenantId);

        assertThat(key)
                .startsWith("tenants/" + tenantId + "/")
                .contains("/contacts/unassigned/")
                .contains("/documents/")
                .endsWith("my_unsafe_file_name_.pdf");
    }

    @Test
    void normalizesContentTypeBeforeVerification() {
        assumeS3IsConfigured();

        assertThat(storageService.normalizeContentType("Application/PDF; charset=utf-8"))
                .isEqualTo("application/pdf");

        assertThat(storageService.isAllowedContentType("Application/PDF; charset=utf-8"))
                .isTrue();
    }

    private void assumeS3IsConfigured() {
        Assumptions.assumeTrue(
                environment.getProperty("storage.s3.bucket") != null
                        && !environment.getProperty("storage.s3.bucket").isBlank()
                        && !environment.getProperty("storage.s3.bucket").contains("${"),
                "Skipping real S3 integration tests because storage.s3.bucket is not configured."
        );

        Assumptions.assumeTrue(
                environment.getProperty("aws.accessKeyId") != null
                        || System.getenv("AWS_ACCESS_KEY_ID") != null,
                "Skipping real S3 integration tests because AWS_ACCESS_KEY_ID is not set."
        );

        Assumptions.assumeTrue(
                environment.getProperty("aws.secretAccessKey") != null
                        || System.getenv("AWS_SECRET_ACCESS_KEY") != null,
                "Skipping real S3 integration tests because AWS_SECRET_ACCESS_KEY is not set."
        );
    }
}