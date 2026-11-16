package com.fincore.server.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Fetches a fixed SEC directory with bounded responses, caching and retained original bytes. */
@Component
public class SecDirectoryClient {
  public static final String SOURCE_URL = "https://www.sec.gov/files/company_tickers.json";
  private static final int MAX_BYTES = 10 * 1024 * 1024;
  private final String userAgent;
  private final Path rawDirectory;
  private final ObjectMapper mapper;
  private final HttpClient client;
  private Directory cached;
  private Instant nextAttempt = Instant.MIN;

  /** Uses explicit SEC identification and persistent local evidence storage. */
  @org.springframework.beans.factory.annotation.Autowired
  public SecDirectoryClient(
      @Value("${fincore.sec.user-agent:}") String userAgent,
      @Value("${fincore.sec.raw-directory:./data/sec-directory}") String rawDirectory,
      ObjectMapper mapper) {
    this(
        userAgent,
        Path.of(rawDirectory),
        mapper,
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build());
  }

  SecDirectoryClient(String userAgent, Path rawDirectory, ObjectMapper mapper, HttpClient client) {
    this.userAgent = userAgent;
    this.rawDirectory = rawDirectory;
    this.mapper = mapper;
    this.client = client;
  }

  /** Coalesces directory refreshes; an expired cache is never silently returned as fresh. */
  public synchronized Directory directory() {
    Instant now = Instant.now();
    if (cached != null && cached.retrievedAt().plus(Duration.ofHours(24)).isAfter(now)) {
      return cached;
    }
    if (userAgent.isBlank()
        || !userAgent.contains("@")
        || userAgent.chars().anyMatch(c -> c < 32 || c > 126)) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Configure SEC_USER_AGENT with application and real contact information");
    }
    if (nextAttempt.isAfter(now)) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "SEC directory temporarily unavailable; retry later");
    }
    nextAttempt = now.plusSeconds(30);
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(SOURCE_URL))
            .timeout(Duration.ofSeconds(20))
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .GET()
            .build();
    CompletableFuture<HttpResponse<byte[]>> pending =
        client.sendAsync(request, info -> new BoundedSubscriber(MAX_BYTES));
    try {
      HttpResponse<byte[]> response = pending.get(25, TimeUnit.SECONDS);
      if (response.statusCode() != 200) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "SEC directory request failed; retry later");
      }
      byte[] bytes = response.body();
      List<Entry> entries = parse(bytes);
      String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      Files.createDirectories(rawDirectory);
      Path file = rawDirectory.resolve(hash + ".json");
      if (!Files.exists(file)) {
        Files.write(file, bytes);
      }
      cached =
          new Directory(
              entries, Instant.now(), file.toAbsolutePath().toUri().toString(), hash, bytes.length);
      return cached;
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw unavailable();
    } catch (IOException | ExecutionException | TimeoutException | NoSuchAlgorithmException error) {
      throw unavailable();
    } finally {
      pending.cancel(true);
    }
  }

  /** Validates the entire SEC directory before publishing a new cached version. */
  List<Entry> parse(byte[] bytes) {
    try {
      var root = mapper.readTree(bytes);
      if (!root.isObject() || root.isEmpty()) {
        throw unavailable();
      }
      List<Entry> result = new ArrayList<>();
      for (var row : root) {
        var cik = row.get("cik_str");
        var ticker = row.get("ticker");
        var name = row.get("title");
        if (cik == null
            || !cik.isIntegralNumber()
            || !cik.canConvertToLong()
            || cik.longValue() < 1
            || cik.longValue() > 9999999999L
            || ticker == null
            || !ticker.isString()
            || ticker.asText().isBlank()
            || ticker.asText().length() > 100
            || name == null
            || !name.isString()
            || name.asText().isBlank()
            || name.asText().length() > 1000) {
          throw unavailable();
        }
        result.add(
            new Entry(
                String.format(Locale.ROOT, "%010d", cik.longValue()),
                ticker.asText(),
                name.asText()));
      }
      return List.copyOf(result);
    } catch (tools.jackson.core.JacksonException error) {
      throw unavailable();
    }
  }

  private ResponseStatusException unavailable() {
    return new ResponseStatusException(
        HttpStatus.BAD_GATEWAY, "SEC directory could not be retrieved, validated or retained");
  }

  /** One observed ticker, company name and normalized CIK association. */
  public record Entry(String cik, String ticker, String name) {}

  /** Immutable cached entries and retained source artifact metadata. */
  public record Directory(
      List<Entry> entries,
      Instant retrievedAt,
      String storageUri,
      String sha256,
      long byteLength) {}
}
