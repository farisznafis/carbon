package com.kct.jcredit.core_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CoreBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreBackendApplication.class, args);
	}

}
