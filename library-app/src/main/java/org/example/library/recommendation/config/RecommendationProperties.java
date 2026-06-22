package org.example.library.recommendation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application.recommendations")
@Getter
@Setter
public class RecommendationProperties {

    private float recencyDecayFactor = 0.1f;
    private float favoriteWeight = 1.0f;
    private float rating5Weight = 0.8f;
    private float rating4Weight = 0.6f;
    private float rating3Weight = 0.4f;
    private float lowRatingWeight = -0.5f;
    private float noRatingWeight = 0.5f;
    
}
