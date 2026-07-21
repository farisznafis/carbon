//必要かどうか要確認

// package com.kct.jcredit.core_backend.config;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
// import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
// import software.amazon.awssdk.regions.Region;
// import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

// @Configuration
// public class DynamoDBConfig {
 
//     @Value("${aws.region:ap-southeast-1}")
//     private String awsRegion;

//     @Bean
//     public DynamoDbClient dynamoDbClient() {
//         System.out.println("Using AWS Toolkit credentials");
//         System.out.println("Region: " + awsRegion);
        
//         return DynamoDbClient.builder()
//                 .region(Region.of(awsRegion))
//                .credentialsProvider(ProfileCredentialsProvider.create("KCT-meyer"))
//                 .build();
//     }

//     @Bean
//     public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
//         return DynamoDbEnhancedClient.builder()
//                 .dynamoDbClient(dynamoDbClient)
//                 .build();
//     }
// }