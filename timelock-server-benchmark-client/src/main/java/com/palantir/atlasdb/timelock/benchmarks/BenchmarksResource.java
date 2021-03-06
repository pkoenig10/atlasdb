/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
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

package com.palantir.atlasdb.timelock.benchmarks;

import java.util.Map;
import java.util.Optional;

import com.palantir.atlasdb.config.AtlasDbConfig;
import com.palantir.atlasdb.factory.TransactionManagers;
import com.palantir.atlasdb.http.UserAgents;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.KvsPutUnlessExistsBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.KvsReadBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.KvsWriteBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.LockAndUnlockContendedBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.LockAndUnlockUncontendedBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.RangeScanDynamicColumnsBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.RangeScanRowsBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.TimestampBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.TransactionReadRowsBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.TransactionWriteBenchmarkContended;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.TransactionWriteDynamicColumnsBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.benchmarks.TransactionWriteRowsBenchmark;
import com.palantir.atlasdb.timelock.benchmarks.schema.BenchmarksSchema;
import com.palantir.atlasdb.transaction.impl.SerializableTransactionManager;
import com.palantir.atlasdb.util.AtlasDbMetrics;

public class BenchmarksResource implements BenchmarksService {

    private final SerializableTransactionManager txnManager;

    public BenchmarksResource(AtlasDbConfig config) {
        this.txnManager = TransactionManagers.builder()
                .config(config)
                .userAgent(UserAgents.DEFAULT_USER_AGENT)
                .globalMetricsRegistry(AtlasDbMetrics.getMetricRegistry())
                .globalTaggedMetricRegistry(AtlasDbMetrics.getTaggedMetricRegistry())
                .addSchemas(BenchmarksSchema.SCHEMA)
                .allowHiddenTableAccess(true)
                .runtimeConfigSupplier(Optional::empty)
                .build().serializable();
    }

    @Override
    public Map<String, Object> transactionWriteRows(int numClients, int numRequestsPerClient, int numRows,
            int dataSize) {
        return TransactionWriteRowsBenchmark.execute(txnManager, numClients, numRequestsPerClient, numRows, dataSize);
    }

    @Override
    public Map<String, Object> transactionWriteDynamicColumns(int numClients, int numRequestsPerClient, int numRows,
            int dataSize) {
        return TransactionWriteDynamicColumnsBenchmark.execute(txnManager, numClients, numRequestsPerClient, numRows,
                dataSize);
    }

    @Override
    public Map<String, Object> transactionWriteContended(int numClients, int numRequestsPerClient) {
        return TransactionWriteBenchmarkContended.execute(txnManager, numClients, numRequestsPerClient);
    }

    @Override
    public Map<String, Object> lockAndUnlockUncontended(int numClients, int numRequestsPerClient) {
        return LockAndUnlockUncontendedBenchmark.execute(txnManager, numClients, numRequestsPerClient);
    }

    @Override
    public Map<String, Object> lockAndUnlockContended(int numClients, int numRequestsPerClient, int numDistinctLocks) {
        return LockAndUnlockContendedBenchmark.execute(txnManager, numClients, numRequestsPerClient, numDistinctLocks);
    }

    @Override
    public Map<String, Object> transactionReadRows(int numClients, int numRequestsPerClient, int numRows,
            int dataSize) {
        return TransactionReadRowsBenchmark.execute(txnManager, numClients, numRequestsPerClient, numRows, dataSize);
    }

    @Override
    public Map<String, Object> kvsWrite(int numClients, int numRequestsPerClient) {
        return KvsWriteBenchmark.execute(txnManager, numClients, numRequestsPerClient);
    }

    @Override
    public Map<String, Object> kvsPutUnlessExists(int numClients, int numRequestsPerClient) {
        return KvsPutUnlessExistsBenchmark.execute(txnManager, numClients, numRequestsPerClient);
    }

    @Override
    public Map<String, Object> kvsRead(int numClients, int numRequestsPerClient) {
        return KvsReadBenchmark.execute(txnManager, numClients, numRequestsPerClient);
    }

    @Override
    public Map<String, Object> timestamp(int numClients, int numRequestsPerClient) {
        return TimestampBenchmark.execute(txnManager, numClients, numRequestsPerClient);
    }

    @Override
    public Map<String, Object> rangeScanRows(int numClients, int numRequestsPerClient, int dataSize, int numRows) {
        return RangeScanRowsBenchmark.execute(txnManager, numClients, numRequestsPerClient, dataSize, numRows);
    }

    @Override
    public Map<String, Object> rangeScanDynamicColumns(int numClients, int numRequestsPerClient, int dataSize,
            int numRows) {
        return RangeScanDynamicColumnsBenchmark.execute(txnManager, numClients, numRequestsPerClient, dataSize,
                numRows);
    }
}
