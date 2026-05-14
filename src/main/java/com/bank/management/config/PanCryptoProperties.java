package com.bank.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.crypto.pan")
public record PanCryptoProperties(String secret) {
}
