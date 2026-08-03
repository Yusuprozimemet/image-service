package hackyourfuture.net.imageservice.tagging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import hackyourfuture.net.imageservice.image.repository.ImageRepository;
import hackyourfuture.net.imageservice.tagging.service.TaggingService;

// Proves the @Async wiring is actually live: tag() must leave the calling thread
// and run on the configured tagging pool. If @Async, @EnableAsync, or the proxy
// ever breaks, tagging silently goes back to blocking every upload — and nothing
// else in the suite would notice.
@SpringBootTest
@Testcontainers
class TaggingAsyncTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    // Loading the image is the first thing tag() does, so this is where we can see
    // which thread the work landed on. Returning empty stops tag() right there.
    @MockitoBean
    private ImageRepository images;

    @Autowired
    private TaggingService tagging;

    @Test
    void tagRunsOnATaggingThreadNotTheCaller() throws Exception {
        CompletableFuture<String> workerThread = new CompletableFuture<>();
        when(images.findById(anyInt())).thenAnswer(invocation -> {
            workerThread.complete(Thread.currentThread().getName());
            return Optional.empty();
        });

        tagging.tag(1);

        // Fails if @Async is not in effect: the work would have run inline and the
        // recorded name would be this test's own thread.
        String worker = workerThread.get(5, TimeUnit.SECONDS);
        assertThat(worker).startsWith("tagging-");
        assertThat(worker).isNotEqualTo(Thread.currentThread().getName());
    }
}