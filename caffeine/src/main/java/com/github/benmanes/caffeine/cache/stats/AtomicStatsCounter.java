/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
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
package com.github.benmanes.caffeine.cache.stats;

import java.util.concurrent.atomic.LongAdder;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import com.github.benmanes.caffeine.cache.stats.Striped128.Summer;

/**
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class AtomicStatsCounter implements StatsCounter {
  private static final Summer SUM_SUCCESS = (stats, loadCount, totalLoadTime) -> {
    stats.loadSuccessCount = loadCount;
    stats.totalLoadTime += totalLoadTime;
  };
  private static final Summer SUM_FAILURE = (stats, loadCount, totalLoadTime) -> {
    stats.loadFailureCount = loadCount;
    stats.totalLoadTime += totalLoadTime;
  };

  private final LongAdder hitCount;
  private final LongAdder missCount;
  private final LongAdder evictionCount;
  private final LoadTimeAdder loadSuccess;
  private final LoadTimeAdder loadFailure;

  /**
   * Constructs an instance with all counts initialized to zero.
   */
  public AtomicStatsCounter() {
    hitCount = new LongAdder();
    missCount = new LongAdder();
    evictionCount = new LongAdder();
    loadSuccess = new LoadTimeAdder();
    loadFailure = new LoadTimeAdder();
  }

  @Override
  public void recordHits(@Nonnegative int count) {
    hitCount.add(count);
  }

  @Override
  public void recordMisses(@Nonnegative int count) {
    missCount.add(count);
  }

  @Override
  public void recordLoadSuccess(long loadTime) {
    loadSuccess.add(1L, loadTime);
  }

  @Override
  public void recordLoadFailure(long loadTime) {
    loadFailure.add(1L, loadTime);
  }

  @Override
  public void recordEviction() {
    evictionCount.increment();
  }

  @Override
  public CacheStats snapshot() {
    CacheStats stats = new CacheStats();
    stats.hitCount += hitCount.sum();
    stats.missCount += missCount.sum();
    stats.evictionCount += evictionCount.sum();
    loadSuccess.sum(stats, SUM_SUCCESS);
    loadFailure.sum(stats, SUM_FAILURE);
    return stats;
  }

  /**
   * Increments all counters by the values in {@code other}.
   *
   * @param other the counter to increment from
   */
  public void incrementBy(@Nonnull StatsCounter other) {
    CacheStats otherStats = other.snapshot();
    hitCount.add(otherStats.hitCount());
    missCount.add(otherStats.missCount());
    evictionCount.add(otherStats.evictionCount());
    loadSuccess.add(otherStats.loadSuccessCount(), 0L);
    loadFailure.add(otherStats.loadFailureCount(), otherStats.totalLoadTime());
  }

  @Override
  public String toString() {
    return snapshot().toString();
  }
}
