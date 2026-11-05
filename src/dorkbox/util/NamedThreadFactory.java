/*
 * Copyright 2024 dorkbox, llc
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
package dorkbox.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread factory that names threads, and optionally sets the group, priority, and daemon state.
 *
 * This is a plain Java replacement for the former Kotlin `NamedThreadFactory`.
 */
public final class NamedThreadFactory implements ThreadFactory {
    public static final String version = "1.0";

    private final String namePrefix;
    private final ThreadGroup group;
    private final int threadPriority;
    private final boolean daemon;

    private final AtomicInteger poolId = new AtomicInteger();

    public
    NamedThreadFactory(final String namePrefix) {
        this(namePrefix, Thread.currentThread()
                             .getThreadGroup(), Thread.NORM_PRIORITY, true);
    }

    public
    NamedThreadFactory(final String namePrefix, final ThreadGroup group) {
        this(namePrefix, group, Thread.NORM_PRIORITY, true);
    }

    public
    NamedThreadFactory(final String namePrefix, final boolean daemon) {
        this(namePrefix, Thread.currentThread()
                             .getThreadGroup(), Thread.NORM_PRIORITY, daemon);
    }

    public
    NamedThreadFactory(final String namePrefix, final ThreadGroup group, final boolean daemon) {
        this(namePrefix, group, Thread.NORM_PRIORITY, daemon);
    }

    public
    NamedThreadFactory(final String namePrefix, final ThreadGroup group, final int threadPriority, final boolean daemon) {
        if (threadPriority < Thread.MIN_PRIORITY) {
            throw new IllegalArgumentException(String.format("Thread priority (%s) must be >= %s", threadPriority, Thread.MIN_PRIORITY));
        }
        if (threadPriority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(String.format("Thread priority (%s) must be <= %s", threadPriority, Thread.MAX_PRIORITY));
        }

        this.namePrefix = namePrefix;
        this.group = group;
        this.threadPriority = threadPriority;
        this.daemon = daemon;
    }

    @Override
    public
    Thread newThread(final Runnable runnable) {
        final Thread thread = new Thread(group, runnable, namePrefix + '-' + poolId.incrementAndGet());
        thread.setDaemon(daemon);
        thread.setPriority(threadPriority);
        return thread;
    }
}
