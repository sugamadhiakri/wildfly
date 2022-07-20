/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.clustering.ejb.infinispan;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Predicate;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.scheduler.LinkedScheduledEntries;
import org.wildfly.clustering.ee.cache.scheduler.LocalScheduler;
import org.wildfly.clustering.ee.cache.scheduler.ScheduledEntries;
import org.wildfly.clustering.ee.cache.scheduler.SortedScheduledEntries;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.scheduler.AbstractCacheEntryScheduler;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.group.Group;

/**
 * Schedules a bean for expiration.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class BeanExpirationScheduler<I, T> extends AbstractCacheEntryScheduler<I, ImmutableBeanEntry<I>> {

    private final BeanFactory<I, T> factory;

    public BeanExpirationScheduler(Group group, Batcher<TransactionBatch> batcher, BeanFactory<I, T> factory, ExpirationConfiguration<T> expiration, BeanRemover<I, T> remover, Duration closeTimeout) {
        this(group.isSingleton() ? new LinkedScheduledEntries<>() : new SortedScheduledEntries<>(), new BeanRemoveTask<>(batcher, expiration, remover), factory, closeTimeout);
    }

    private <RT extends Predicate<I> & Function<ImmutableBeanEntry<I>, Duration>> BeanExpirationScheduler(ScheduledEntries<I, Instant> entries, RT removeTask, BeanFactory<I, T> factory, Duration closeTimeout) {
        super(new LocalScheduler<>(entries, removeTask, closeTimeout), removeTask, Duration::isNegative, ImmutableBeanEntry::getLastAccessedTime);
        this.factory = factory;
    }

    @Override
    public void schedule(I id) {
        BeanEntry<I> entry = this.factory.findValue(id);
        if (entry != null) {
            this.schedule(id, entry);
        }
    }

    private static class BeanRemoveTask<I, T> implements Predicate<I>, Function<ImmutableBeanEntry<I>, Duration> {
        private final Batcher<TransactionBatch> batcher;
        private final ExpirationConfiguration<T> expiration;
        private final BeanRemover<I, T> remover;

        BeanRemoveTask(Batcher<TransactionBatch> batcher, ExpirationConfiguration<T> expiration, BeanRemover<I, T> remover) {
            this.batcher = batcher;
            this.expiration = expiration;
            this.remover = remover;
        }

        @Override
        public boolean test(I id) {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Expiring stateful session bean %s", id);
            try (Batch batch = this.batcher.createBatch()) {
                try {
                    this.remover.remove(id, this.expiration.getRemoveListener());
                    return true;
                } catch (RuntimeException e) {
                    batch.discard();
                    throw e;
                }
            } catch (RuntimeException e) {
                InfinispanEjbLogger.ROOT_LOGGER.failedToExpireBean(e, id);
                return false;
            }
        }

        @Override
        public Duration apply(ImmutableBeanEntry<I> entry) {
            return this.expiration.getTimeout();
        }
    }
}
