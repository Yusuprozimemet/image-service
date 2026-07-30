package hackyourfuture.net.imageservice.image.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import hackyourfuture.net.imageservice.shared.ApiException;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// Stores and gets image bytes in the Backblaze B2 bucket. If B2 isn't configured
// (no keys), the app still boots and storage calls return 503.
@Service
public class FileService {

    private final S3Client s3;
    private final String bucket;

    public FileService(@Value("${app.b2.endpoint}") String endpoint,
                       @Value("${app.b2.region}") String region,
                       @Value("${app.b2.bucket}") String bucket,
                       @Value("${app.b2.access-key}") String accessKey,
                       @Value("${app.b2.secret-key}") String secretKey) {
        this.bucket = bucket;
        this.s3 = isConfigured(accessKey, secretKey, bucket)
                ? buildClient(endpoint, region, accessKey, secretKey)
                : null;
    }

    // Save the bytes under the key.
    public void put(String key, byte[] data, String contentType) {
        client().putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data));
    }

    // Get the bytes for the key.
    public byte[] get(String key) {
        return client().getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .asByteArray();
    }

    // Delete the object.
    public void delete(String key) {
        client().deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    private S3Client client() {
        if (s3 == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "image storage is not configured");
        }
        return s3;
    }

    private static boolean isConfigured(String accessKey, String secretKey, String bucket) {
        return !accessKey.isBlank() && !secretKey.isBlank() && !bucket.isBlank();
    }

    private static S3Client buildClient(String endpoint, String region, String accessKey, String secretKey) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                // B2 needs path-style URLs.
                .forcePathStyle(true)
                .build();
    }
}
