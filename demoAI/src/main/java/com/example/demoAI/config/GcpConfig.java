//package com.example.demoAI.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//
//import javax.annotation.PostConstruct;
//
//@Configuration
//public class GcpConfig {
//
//    @Value("${google.application.credentials}")
//    private String googleApplicationCredentials;
//
//    // Đặt đường dẫn tới tệp keyfile làm thuộc tính hệ thống
//    @PostConstruct
//    public void setGoogleApplicationCredentials() {
//        System.out.println(googleApplicationCredentials + "aaaaa");
//        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", googleApplicationCredentials);
//    }
//}