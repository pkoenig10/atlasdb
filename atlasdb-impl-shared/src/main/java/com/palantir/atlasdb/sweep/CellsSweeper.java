/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.sweep;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.palantir.atlasdb.cleaner.Follower;
import com.palantir.atlasdb.keyvalue.api.Cell;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.logging.LoggingArgs;
import com.palantir.atlasdb.transaction.api.Transaction;
import com.palantir.atlasdb.transaction.api.TransactionManager;
import com.palantir.logsafe.Arg;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.UnsafeArg;

public class CellsSweeper {
    private static final Logger log = LoggerFactory.getLogger(CellsSweeper.class);

    private final TransactionManager txManager;
    private final KeyValueService keyValueService;
    private final Collection<Follower> followers;
    private final PersistentLockManager persistentLockManager;

    public CellsSweeper(
            TransactionManager txManager,
            KeyValueService keyValueService,
            PersistentLockManager persistentLockManager,
            Collection<Follower> followers) {
        this.txManager = txManager;
        this.keyValueService = keyValueService;
        this.followers = followers;
        this.persistentLockManager = persistentLockManager;
    }

    public void sweepCells(
            TableReference tableRef,
            Multimap<Cell, Long> cellTsPairsToSweep,
            Collection<Cell> sentinelsToAdd) {
        if (cellTsPairsToSweep.isEmpty()) {
            log.info("Attempted to delete 0 cell+timestamp pairs from table {}.",
                    LoggingArgs.tableRef(tableRef));
            return;
        }

        log.info("Attempted to delete {} stale cell+timestamp pairs from table {}, and add {} sentinels.",
                SafeArg.of("numCellTsPairsToDelete", cellTsPairsToSweep.size()),
                LoggingArgs.tableRef(tableRef),
                SafeArg.of("numGarbageCollectionSentinelsToAdd", sentinelsToAdd.size()));

        for (Follower follower : followers) {
            follower.run(txManager, tableRef, cellTsPairsToSweep.keySet(), Transaction.TransactionType.HARD_DELETE);
        }

        if (!sentinelsToAdd.isEmpty()) {
            keyValueService.addGarbageCollectionSentinelValues(
                    tableRef,
                    sentinelsToAdd);
        }

        if (cellTsPairsToSweep.entries().stream().anyMatch(entry -> entry.getValue() == null)) {
            log.error("When sweeping table {} found cells to sweep with the start timestamp null."
                            + " This is unexpected. The cellTs pairs to sweep were: {}.",
                    LoggingArgs.tableRef(tableRef),
                    getLoggingArgForCells(tableRef, cellTsPairsToSweep));
        }

        persistentLockManager.acquirePersistentLockWithRetry();

        try {
            keyValueService.delete(tableRef, cellTsPairsToSweep);
        } finally {
            persistentLockManager.releasePersistentLock();
        }
    }

    private Arg<String> getLoggingArgForCells(TableReference tableRef,  Multimap<Cell, Long> cellTsPairsToSweep) {
        String message = getMessage(cellTsPairsToSweep);

        if (allComponentsAreSafe(tableRef, cellTsPairsToSweep)) {
            return SafeArg.of("cellTsPairsToSweep", message);
        } else {
            return UnsafeArg.of("cellTsPairsToSweep", message);
        }
    }

    private boolean allComponentsAreSafe(TableReference tableRef, Multimap<Cell, Long> cellTsPairsToSweep) {
        // all or nothing
        return cellTsPairsToSweep.keySet().stream().allMatch(cell -> LoggingArgs.isSafeForLogging(tableRef, cell));
    }

    private <T> String getMessage(Multimap<Cell, Long> cellTsPairsToSweep) {
        return cellTsPairsToSweep.entries().stream()
                .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                .map(entry -> entry.getKey().toString() + "->" + entry.getValue())
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
