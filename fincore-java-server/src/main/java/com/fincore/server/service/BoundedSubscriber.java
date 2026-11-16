package com.fincore.server.service;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Cancels upstream delivery before a response can exceed its permitted memory budget. */
final class BoundedSubscriber implements HttpResponse.BodySubscriber<byte[]> {
  private final int limit;
  private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
  private final CompletableFuture<byte[]> result = new CompletableFuture<>();
  private Flow.Subscription subscription;

  BoundedSubscriber(int limit) {
    this.limit = limit;
  }

  @Override
  public CompletionStage<byte[]> getBody() {
    return result;
  }

  @Override
  public void onSubscribe(Flow.Subscription upstream) {
    subscription = upstream;
    upstream.request(1);
  }

  @Override
  public void onNext(List<ByteBuffer> buffers) {
    for (ByteBuffer buffer : buffers) {
      if (buffer.remaining() > limit - bytes.size()) {
        subscription.cancel();
        result.completeExceptionally(new IllegalStateException("SEC response exceeds size limit"));
        return;
      }
      byte[] chunk = new byte[buffer.remaining()];
      buffer.get(chunk);
      bytes.writeBytes(chunk);
    }
    subscription.request(1);
  }

  @Override
  public void onError(Throwable error) {
    result.completeExceptionally(error);
  }

  @Override
  public void onComplete() {
    result.complete(bytes.toByteArray());
  }
}
