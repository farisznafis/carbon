package com.kct.jcredit.core_backend.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;

/**
 * Amazon SES (v2) クライアント。
 * app.mail.provider=ses のときだけ生成される。ローカル (MailHog/SMTP) では生成されない。
 *
 * 認証情報とリージョンは DynamoDB / S3 と同じ spring-cloud-aws の解決結果を再利用するため、
 * AWS_PROFILE / 環境変数 / EC2・ECS のインスタンスロールがそのまま使える。
 */
@Configuration
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "ses")
public class SesConfig {

    @Bean(destroyMethod = "close")
    public SesV2Client sesV2Client(
            ObjectProvider<AwsCredentialsProvider> credentialsProvider,
            ObjectProvider<AwsRegionProvider> regionProvider,
            AppMailProperties mailProperties) {

        SesV2ClientBuilder builder = SesV2Client.builder()
                .credentialsProvider(credentialsProvider.getIfAvailable(DefaultCredentialsProvider::create));

        Region region = resolveRegion(mailProperties.getSes().getRegion(), regionProvider);
        if (region != null) {
            builder.region(region);
        }
        return builder.build();
    }

    private Region resolveRegion(String configured, ObjectProvider<AwsRegionProvider> regionProvider) {
        if (configured != null && !configured.isBlank()) {
            return Region.of(configured.trim());
        }
        AwsRegionProvider provider = regionProvider.getIfAvailable();
        return provider != null ? provider.getRegion() : null;
    }
}
