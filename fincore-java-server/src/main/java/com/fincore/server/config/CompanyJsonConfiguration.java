package com.fincore.server.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;

/** Strict JSON contracts prevent numeric-to-string coercion of exact financial values. */
@Configuration
public class CompanyJsonConfiguration {
  /** Rejects unknown fields and scalar coercions in creation requests. */
  @Bean
  JsonMapperBuilderCustomizer companyJsonCustomizer() {
    return builder ->
        builder
            .withCoercionConfig(
                tools.jackson.databind.type.LogicalType.Textual,
                config ->
                    config
                        .setCoercion(
                            tools.jackson.databind.cfg.CoercionInputShape.Integer,
                            tools.jackson.databind.cfg.CoercionAction.Fail)
                        .setCoercion(
                            tools.jackson.databind.cfg.CoercionInputShape.Float,
                            tools.jackson.databind.cfg.CoercionAction.Fail)
                        .setCoercion(
                            tools.jackson.databind.cfg.CoercionInputShape.Boolean,
                            tools.jackson.databind.cfg.CoercionAction.Fail))
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }
}
