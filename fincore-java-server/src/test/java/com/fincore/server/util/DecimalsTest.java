package com.fincore.server.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DecimalsTest {
  @Test
  void precisionIsExactAndDoesNotRound() {
    assertThat(Decimals.parse("123456789012345678901234567890123456789012.123456789012345678"))
        .isEqualByComparingTo(
            new BigDecimal("123456789012345678901234567890123456789012.123456789012345678"));
    assertThatThrownBy(() -> Decimals.parse("0.1234567890123456789"))
        .isInstanceOf(ArithmeticException.class);
    assertThatThrownBy(() -> Decimals.parse("1000000000000000000000000000000000000000000"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
