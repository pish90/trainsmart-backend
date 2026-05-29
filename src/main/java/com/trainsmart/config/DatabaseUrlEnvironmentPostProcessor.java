package com.trainsmart.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.startsWith("jdbc:")) {
            environment.getPropertySources().addFirst(
                new MapPropertySource("railwayDatasourceUrl",
                    Map.of("spring.datasource.url", "jdbc:" + databaseUrl))
            );
        }
    }
}
