package com.fincore.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class SecDirectoryClientTest {
  private static final byte[] VALID =
      ("{\"0\":{\"cik_str\":320193,\"ticker\":\"AAPL\"," + "\"title\":\"Apple Inc.\"}}")
          .getBytes(StandardCharsets.UTF_8);
  @TempDir Path raw;

  @Test
  void retainsOriginalBytesAndReusesCachedDirectory() throws Exception {
    HttpClient http = mock(HttpClient.class);
    var reply = response(200, VALID);
    when(http.sendAsync(
            any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
        .thenReturn(reply);
    var client = new SecDirectoryClient("FinCore real@example.org", raw, new ObjectMapper(), http);
    var first = client.directory();
    assertThat(first.entries().getFirst().cik()).isEqualTo("0000320193");
    assertThat(Files.readAllBytes(Path.of(java.net.URI.create(first.storageUri()))))
        .isEqualTo(VALID);
    assertThat(client.directory()).isSameAs(first);
    verify(http, times(1)).sendAsync(any(), any());
  }

  @Test
  void rejectsMissingContactWithoutMakingRequests() {
    HttpClient http = mock(HttpClient.class);
    var client = new SecDirectoryClient("", raw, new ObjectMapper(), http);
    assertThatThrownBy(client::directory)
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("503");
    verifyNoInteractions(http);
  }

  @Test
  void upstreamFailureIsSanitizedAndBacksOff() {
    HttpClient http = mock(HttpClient.class);
    var reply = response(429, new byte[0]);
    when(http.sendAsync(
            any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
        .thenReturn(reply);
    var client = new SecDirectoryClient("FinCore real@example.org", raw, new ObjectMapper(), http);
    assertThatThrownBy(client::directory)
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("502");
    assertThatThrownBy(client::directory)
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("503");
    verify(http, times(1)).sendAsync(any(), any());
  }

  @Test
  void transportFailureAndMalformedJsonDoNotPublishDirectory() {
    HttpClient http = mock(HttpClient.class);
    when(http.sendAsync(
            any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
        .thenReturn(CompletableFuture.failedFuture(new IOException("private transport data")));
    var client = new SecDirectoryClient("FinCore real@example.org", raw, new ObjectMapper(), http);
    assertThatThrownBy(client::directory)
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageNotContaining("private transport data");
    assertThatThrownBy(() -> client.parse("{}".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> client.parse("{\"0\":{}}".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void oversizedResponseCancelsSubscriptionBeforeRetainingExcessBytes() {
    var subscriber = new BoundedSubscriber(3);
    Flow.Subscription subscription = mock(Flow.Subscription.class);
    subscriber.onSubscribe(subscription);
    subscriber.onNext(
        List.of(ByteBuffer.wrap(new byte[] {1, 2}), ByteBuffer.wrap(new byte[] {3, 4})));
    verify(subscription).cancel();
    assertThat(subscriber.getBody().toCompletableFuture()).isCompletedExceptionally();
  }

  @Test
  void rejectsOversizedCikIntegersInsteadOfTruncatingThem() {
    var client = new SecDirectoryClient("", raw, new ObjectMapper(), mock(HttpClient.class));
    byte[] source =
        new String(VALID, StandardCharsets.UTF_8)
            .replace("320193", "18446744073709871809")
            .getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> client.parse(source)).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void boundedWaitCancelsTimedOutRequest() throws Exception {
    HttpClient http = mock(HttpClient.class);
    CompletableFuture<HttpResponse<byte[]>> pending = mock(CompletableFuture.class);
    when(pending.get(25, java.util.concurrent.TimeUnit.SECONDS))
        .thenThrow(new java.util.concurrent.TimeoutException());
    when(http.sendAsync(
            any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
        .thenReturn(pending);
    var client = new SecDirectoryClient("FinCore real@example.org", raw, new ObjectMapper(), http);
    assertThatThrownBy(client::directory).isInstanceOf(ResponseStatusException.class);
    verify(pending).cancel(true);
  }

  @SuppressWarnings("unchecked")
  private CompletableFuture<HttpResponse<byte[]>> response(int status, byte[] body) {
    HttpResponse<byte[]> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.body()).thenReturn(body);
    return CompletableFuture.completedFuture(response);
  }
}
