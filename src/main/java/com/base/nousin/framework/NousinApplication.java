package com.base.nousin.framework;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 */
@SpringBootApplication(scanBasePackages="com.base.nousin")
@MapperScan("com.base.nousin.web.dao")
public class NousinApplication {

	public static void main(String[] args) {
		SpringApplication.run(NousinApplication.class, args);
	}

}
