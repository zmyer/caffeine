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

import java.util.concurrent.locks.StampedLock;

/**
 * @author ben.manes@gmail.com (Ben Manes)
 */
final class LoadTimeAdder extends Striped128 {

  @Override
  protected AdderSegment create(long loadCount, long loadTime) {
    return new AdderSegment(loadCount, loadTime);
  }

  private static final class AdderSegment extends StampedLock implements Striped128.Segment {
    private static final long serialVersionUID = 1L;

    /**
     * The number of times to spin (doing nothing except polling a memory location) before giving up
     * while waiting to perform an operation. Should be one on uniprocessors. On multiprocessors,
     * this value should be large enough so that two threads exchanging items as fast as possible
     * block only when one of them is stalled (due to GC or preemption), but not much longer, to
     * avoid wasting CPU resources. Seen differently, this value is a little over half the number of
     * cycles of an average context switch time on most systems. The value here is approximately the
     * average of those across a range of tested systems.
     */
    static final int SPINS = (NCPU == 1) ? 1 : 2000;

    long loadCount;
    long totalLoadTime;

    AdderSegment(long loadCount, long loadTime) {
      this.totalLoadTime = loadTime;
      this.loadCount = loadCount;
    }

    @Override
    public boolean tryAdd(long loadCount, long loadTime) {
      for (int spins = 0; spins < SPINS; spins++) {
        long stamp = tryWriteLock();
        if (stamp != 0) {
          this.totalLoadTime += loadTime;
          this.loadCount += loadCount;
          unlockWrite(stamp);
          return true;
        }
      }
      return false;
    }

    @Override
    public void sum(CacheStats stats, Summer summer) {
      long loadCount, totalLoadTime;

      for (int spins = 0; spins < SPINS; spins++) {
        long stamp = tryOptimisticRead();
        totalLoadTime = this.totalLoadTime;
        loadCount = this.loadCount;
        if (validate(stamp)) {
          summer.accept(stats, loadCount, totalLoadTime);
          return;
        }
      }

      long stamp = readLock();
      totalLoadTime = this.totalLoadTime;
      loadCount = this.loadCount;
      unlockRead(stamp);
      summer.accept(stats, loadCount, totalLoadTime);
    }
  }
}
