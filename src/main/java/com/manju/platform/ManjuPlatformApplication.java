package com.manju.platform;

import com.manju.platform.service.AIService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ManjuPlatformApplication {

	public static void main(String[] args) {
//		AIService.enableTestMode();   // 启用测试模式（供Jmeter使用）
		SpringApplication.run(ManjuPlatformApplication.class, args);
	}

}
