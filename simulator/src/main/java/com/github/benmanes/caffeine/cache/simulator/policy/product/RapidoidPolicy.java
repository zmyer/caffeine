/*
 * Copyright 2017 Ben Manes. All Rights Reserved.
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
package com.github.benmanes.caffeine.cache.simulator.policy.product;

import java.util.Set;

import org.rapidoid.cache.Cache;
import org.rapidoid.cache.Caching;

import com.github.benmanes.caffeine.cache.simulator.BasicSettings;
import com.github.benmanes.caffeine.cache.simulator.policy.Policy;
import com.github.benmanes.caffeine.cache.simulator.policy.PolicyStats;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;

/**
 * Rapidoid cache implementation.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class RapidoidPolicy implements Policy {
  private final Cache<Object, Object> cache;
  private final PolicyStats policyStats;
  private final int maximumSize;

  public RapidoidPolicy(Config config) {
    policyStats = new PolicyStats("product.Rapidoid");
    BasicSettings settings = new BasicSettings(config);
    maximumSize = settings.maximumSize();
    cache = Caching.of(Object.class, Object.class)
        .capacity(maximumSize)
        .build();
  }

  /** Returns all variations of this policy based on the configuration parameters. */
  public static Set<Policy> policies(Config config) {
    return ImmutableSet.of(new RapidoidPolicy(config));
  }

  @Override
  public void record(long key) {
    Object value = cache.getIfExists(key);
    if (value == null) {
      if (cache.size() == maximumSize) {
        policyStats.recordEviction();
      }
      cache.set(key, key);
      policyStats.recordMiss();
    } else {
      policyStats.recordHit();
    }
  }

  @Override
  public PolicyStats stats() {
    return policyStats;
  }
}
