package com.fincore.server.config;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import tools.jackson.databind.json.JsonMapper;

/** Uses Jackson 3 for PostgreSQL JSONB while preserving structured values. */
public class CompanyJsonFormatMapper implements FormatMapper {
  private final JsonMapper mapper = JsonMapper.builder().build();

  @Override
  public <T> T fromString(CharSequence value, JavaType<T> javaType, WrapperOptions options) {
    return mapper.readValue(value.toString(), mapper.constructType(javaType.getJavaType()));
  }

  @Override
  public <T> String toString(T value, JavaType<T> javaType, WrapperOptions options) {
    return mapper.writeValueAsString(value);
  }
}
