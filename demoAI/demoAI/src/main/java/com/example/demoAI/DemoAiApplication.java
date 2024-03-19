package com.example.demoAI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;

@SpringBootApplication
public class DemoAiApplication {

	public static void main(String[] args) {

		String keyfilePath = Paths.get("demoAI/src", "main", "resources", "keyfile1.json").toAbsolutePath().toString();

		// Đặt đường dẫn tới tệp keyfile làm thuộc tính hệ thống

		System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", keyfilePath);

		SpringApplication.run(DemoAiApplication.class, args);
	}

}
