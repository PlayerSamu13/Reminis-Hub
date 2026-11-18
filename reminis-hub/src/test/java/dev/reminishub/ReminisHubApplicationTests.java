package dev.reminishub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "reminishub.jwt.secret=" + SecretosDePrueba.JWT)
class ReminisHubApplicationTests {

    @Test
    void contextLoads() {
    }

}
