package com.fincore.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the FinCore API server. */
@SpringBootApplication
public class FincoreJavaServerApplication {

  /**
   * Launches the Spring Boot application.
   *
   * @param args command-line application arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(FincoreJavaServerApplication.class, args);
  }
}
