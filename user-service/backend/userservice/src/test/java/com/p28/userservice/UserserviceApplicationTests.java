package com.p28.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserserviceApplicationTests {

	@Test
	void contextLoads() {
	}

	static {
        System.out.println("MONGODB_URI: " + System.getenv("MONGODB_URI"));
        System.out.println("PORT: " + System.getenv("PORT"));
    }
}
