package com.fincore.server.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact financial decimal conversion without implicit rounding. */
public final class Decimals {
  private Decimals() {}

  /** Converts a validated JSON string into the storage precision. */
  public static BigDecimal parse(String value) {
    if (value == null) {
      return null;
    }
    BigDecimal result = new BigDecimal(value).setScale(18, RoundingMode.UNNECESSARY);
    if (result.precision() > 60) {
      throw new IllegalArgumentException("Decimal exceeds 42 integer and 18 fractional digits");
    }
    return result;
  }
}
