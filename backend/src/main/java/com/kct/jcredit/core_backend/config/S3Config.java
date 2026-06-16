//必要かどうか要確認

// package com.kct.jcredit.core_backend.config;


// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
// import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
// import software.amazon.awssdk.regions.Region;
// import software.amazon.awssdk.services.s3.S3Client;

// @Configuration
// public class S3Config {

//     @Value("${aws.region:ap-southeast-1}")
//     private String awsRegion;

//     @Value("${aws.profile:default}")
//     private String awsProfile;

//     @Bean
//     public S3Client s3Client() {
//         return S3Client.builder()
//                 .region(Region.of(awsRegion))
//                 .credentialsProvider(ProfileCredentialsProvider.create("KCT-meyer")) 
//                 .build();
//     }

// }