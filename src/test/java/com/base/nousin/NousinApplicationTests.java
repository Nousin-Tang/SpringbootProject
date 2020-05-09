package com.base.nousin;

import com.base.nousin.framework.NousinApplication;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NousinApplication.class)
class NousinApplicationTests {

	@Test
	void contextLoads() {
		System.out.println("12");
	}

}
