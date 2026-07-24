package hackyourfuture.net.practice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Boots the full context against a real PostgreSQL started by Testcontainers.
// @ServiceConnection wires the container's JDBC details into Spring, so
// DataSourceAutoConfiguration gets a real datasource — no exclude needed.
@SpringBootTest
@Testcontainers
class PracticeApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Test
	void contextLoads() {
	}

}
