package com.aryafatthurahman.backend.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Typed, validated application configuration. Environment variables can override
 * every leaf through Quarkus' standard config precedence rules.
 */
@ConfigMapping(prefix = "app")
public interface AppConfig {

    Pricing pricing();

    Orders orders();

    Cache cache();

    interface Pricing {
        @WithDefault("0.0825")
        BigDecimal taxRate();
    }

    interface Orders {
        @WithDefault("USD")
        String currency();

        @WithDefault("100")
        int maxItemsPerOrder();
    }

    interface Cache {
        @WithDefault("true")
        boolean enabled();

        @WithDefault("true")
        boolean redisEnabled();

        @WithDefault("30s")
        Duration ttl();
    }
}
