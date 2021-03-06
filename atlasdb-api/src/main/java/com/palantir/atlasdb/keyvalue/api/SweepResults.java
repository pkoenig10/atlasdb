/*
 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
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
package com.palantir.atlasdb.keyvalue.api;

import java.util.Optional;

import org.immutables.value.Value;

import com.palantir.atlasdb.encoding.PtBytes;

@Value.Immutable
public abstract class SweepResults {

    @Value.Default
    public Optional<byte[]> getPreviousStartRow() {
        return Optional.empty();
    }

    @Value.Default
    public Optional<byte[]> getNextStartRow() {
        return Optional.empty();
    }

    /**
     * The approximate number of (cell, timestamp) pairs examined.
     */
    public abstract long getCellTsPairsExamined();

    /**
     * The number of (cell, timestamp) pairs deleted.
     */
    public abstract long getStaleValuesDeleted();

    /**
     * The minimum sweep timestamp while sweeping this table.
     */
    public abstract long getMinSweptTimestamp();

    /**
     * Time spent sweeping this iteration in milliseconds.
     */
    public abstract long getTimeInMillis();

    /**
     * Time in milliseconds when we started sweeping this table.
     */
    @Value.Auxiliary
    public abstract long getTimeSweepStarted();

    public long getTimeElapsedSinceStartedSweeping() {
        return System.currentTimeMillis() - getTimeSweepStarted();
    }

    /**
     * Returns a new {@link SweepResults} representing cumulative results from this instance and {@code other}.
     * The operation is commutative.
     */
    public SweepResults accumulateWith(SweepResults other) {
        return SweepResults.builder()
                .nextStartRow(maxRowOptional(getNextStartRow(), other.getNextStartRow()))
                .cellTsPairsExamined(getCellTsPairsExamined() + other.getCellTsPairsExamined())
                .staleValuesDeleted(getStaleValuesDeleted() + other.getStaleValuesDeleted())
                .minSweptTimestamp(Math.min(getMinSweptTimestamp(), other.getMinSweptTimestamp()))
                .timeInMillis(getTimeInMillis() + other.getTimeInMillis())
                .timeSweepStarted(Math.min(getTimeSweepStarted(), other.getTimeSweepStarted()))
                .build();
    }

    private Optional<byte[]> maxRowOptional(Optional<byte[]> fst, Optional<byte[]> snd) {
        return fst.flatMap(row1 -> snd.map(row2 -> PtBytes.BYTES_COMPARATOR.max(row1, row2)));
    }

    public static ImmutableSweepResults.Builder builder() {
        return ImmutableSweepResults.builder();
    }

    public static SweepResults createEmptySweepResultWithMoreToSweep() {
        return createEmptySweepResult(Optional.of(PtBytes.EMPTY_BYTE_ARRAY));
    }

    public static SweepResults createEmptySweepResultWithNoMoreToSweep() {
        return createEmptySweepResult(Optional.empty());
    }

    public static SweepResults createEmptySweepResult(Optional<byte[]> startRow) {
        return builder()
                .cellTsPairsExamined(0)
                .staleValuesDeleted(0)
                .minSweptTimestamp(Long.MAX_VALUE)
                .nextStartRow(startRow)
                .timeInMillis(0)
                .timeSweepStarted(System.currentTimeMillis())
                .build();
    }

}
