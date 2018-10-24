/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.enterprise.cloudsearch.sharepoint;

import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Executor;

/**
 * An abstract CacheLoader whose reload() is asynchronous (unlike the default).
 */
abstract class AsyncCacheLoader<K, V> extends CacheLoader<K, V> {
  protected abstract Executor executor();

  @Override
  public ListenableFuture<V> reload(final K key, V oldValue) {
    final SettableFuture<V> future = SettableFuture.create();
    executor().execute(() -> {
      try {
        future.set(load(key));
      } catch (Throwable t) {
        future.setException(t);
      }
    });
    return future;
  }
}
