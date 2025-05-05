package com.ssafy.backend;

import com.ssafy.backend.config.DotenvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		DotenvConfig.loadEnv();
		SpringApplication.run(BackendApplication.class, args);
	}

}
