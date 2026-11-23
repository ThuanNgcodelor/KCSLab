package com.example.app.notification.customer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(CustomerNotificationProperties.class)
public class CustomerNotificationConfiguration {}
