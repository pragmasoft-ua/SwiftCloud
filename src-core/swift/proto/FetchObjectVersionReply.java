/*****************************************************************************
 * Copyright 2011-2012 INRIA
 * Copyright 2011-2012 Universidade Nova de Lisboa
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
 *****************************************************************************/
package swift.proto;

import swift.clocks.CausalityClock;
import swift.crdt.core.CRDT;
import swift.crdt.core.ManagedCRDT;
import sys.net.api.rpc.RpcHandle;
import sys.net.api.rpc.RpcHandler;
import sys.net.api.rpc.RpcMessage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

/**
 * Server reply to object version fetch request.
 * 
 * @author mzawirski
 */
public class FetchObjectVersionReply implements RpcMessage, MetadataSamplable {
    public enum FetchStatus {
        /**
         * The reply contains requested version.
         */
        OK,
        /**
         * The requested object is not in the store.
         */
        OBJECT_NOT_FOUND,
        /**
         * The requested version of an object is available in the store.
         */
        VERSION_NOT_FOUND
    }

    protected FetchStatus status;
    protected ManagedCRDT<?> crdt;
    protected CausalityClock estimatedLatestKnownClock;
    protected CausalityClock estimatedDisasterDurableLatestKnownClock;

    // public Map<String, Object> staleReadsInfo;

    // Fake constructor for Kryo serialization. Do NOT use.
    FetchObjectVersionReply() {
    }

    public FetchObjectVersionReply(FetchStatus status, ManagedCRDT<?> crdt, CausalityClock estimatedLatestKnownClock,
            CausalityClock estimatedDisasterDurableLatestKnownClock) {

        this.crdt = crdt;
        this.status = status;
        this.estimatedLatestKnownClock = estimatedLatestKnownClock;
        this.estimatedDisasterDurableLatestKnownClock = estimatedDisasterDurableLatestKnownClock;
        if (estimatedLatestKnownClock != null && estimatedDisasterDurableLatestKnownClock != null) {
            // TODO: use diff over here?
            this.estimatedDisasterDurableLatestKnownClock.intersect(estimatedLatestKnownClock);
        }
    }

    // public FetchObjectVersionReply(FetchStatus status, ManagedCRDT<?> crdt,
    // CausalityClock estimatedLatestKnownClock,
    // CausalityClock estimatedDisasterDurableLatestKnownClock, Map<String,
    // Object> staleReadsInfo) {
    //
    // this(status, crdt, estimatedLatestKnownClock,
    // estimatedDisasterDurableLatestKnownClock);
    //
    // // EVALUATION
    // this.staleReadsInfo = staleReadsInfo;
    // }

    /**
     * @return status code of the reply
     */
    public FetchStatus getStatus() {
        return status;
    }

    /**
     * @return state of an object requested by the client; null if
     *         {@link #getStatus()} is {@link FetchStatus#OBJECT_NOT_FOUND}.
     */
    // Old docs, not true anymore: if {@link #getStatus()} is {@link
    // FetchStatus#OK} then the object is
    // pruned from history at most to the level specified by version in
    // the original client request;
    public ManagedCRDT<?> getCrdt() {
        return crdt;
    }

    /**
     * @return estimation of the latest committed clock in the store
     */
    public CausalityClock getEstimatedCommittedVersion() {
        return estimatedLatestKnownClock;
    }

    /**
     * @return estimation of the latest committed clock in the store, durable
     *         even in case of distaster affecting fragment of the store
     */
    public CausalityClock getEstimatedDisasterDurableCommittedVersion() {
        return estimatedDisasterDurableLatestKnownClock;
    }

    @Override
    public void deliverTo(RpcHandle conn, RpcHandler handler) {
        if (handler != RpcHandler.NONE)
            ((SwiftProtocolHandler) handler).onReceive(conn, this);
    }

    @Override
    public void recordMetadataSample(MetadataStatsCollector collector) {
        if (!collector.isEnabled()) {
            return;
        }
        Kryo kryo = collector.getFreshKryo();
        Output buffer = collector.getFreshKryoBuffer();

        kryo.writeObject(buffer, this);
        final int totalSize = buffer.position();
        buffer.clear();

        int maxExceptionsNum = 0;
        int maxVectorSize = 0;
        if (estimatedDisasterDurableLatestKnownClock != null) {
            maxExceptionsNum = Math.max(estimatedDisasterDurableLatestKnownClock.getExceptionsNumber(),
                    maxExceptionsNum);
            maxVectorSize = Math.max(estimatedDisasterDurableLatestKnownClock.getSize(), maxVectorSize);
        }
        if (estimatedLatestKnownClock != null) {
            maxExceptionsNum = Math.max(estimatedLatestKnownClock.getExceptionsNumber(), maxExceptionsNum);
            maxVectorSize = Math.max(estimatedLatestKnownClock.getSize(), maxVectorSize);
        }

        int versionSize = 0;
        int valueSize = 0;
        if (crdt != null) {
            maxExceptionsNum = Math.max(crdt.getClock().getExceptionsNumber(), maxExceptionsNum);

            kryo = collector.getFreshKryo();
            buffer = collector.getFreshKryoBuffer();
            kryo.writeObject(buffer, crdt.getUID());
            // TODO: be more precise w.r.t version (we should get the requested
            // version, not the latest)
            final CRDT version = crdt.getLatestVersion(null);
            kryo.writeObject(buffer, version);
            versionSize = buffer.position();

            kryo = collector.getFreshKryo();
            buffer = collector.getFreshKryoBuffer();
            kryo.writeObject(buffer, crdt.getUID());
            final Object value = version.getValue();
            if (value != null) {
                kryo.writeObject(buffer, value);
            } else {
                kryo.writeObject(buffer, false);
            }
            valueSize = buffer.position();
        }

        kryo = collector.getFreshKryo();
        buffer = collector.getFreshKryoBuffer();
        if (estimatedDisasterDurableLatestKnownClock != null) {
            kryo.writeObject(buffer, estimatedDisasterDurableLatestKnownClock);
        }
        if (estimatedLatestKnownClock != null) {
            kryo.writeObject(buffer, estimatedLatestKnownClock);
        }
        if (crdt != null) {
            kryo.writeObject(buffer, crdt.getClock());
            kryo.writeObject(buffer, crdt.getPruneClock());
            kryo.writeObject(buffer, crdt.getUpdatesTimestampMappingsSince(crdt.getPruneClock()));
        }
        final int globalMetadata = buffer.position();

        collector.recordStats(this, totalSize, versionSize, valueSize, globalMetadata, 1, maxVectorSize,
                maxExceptionsNum);
    }
}
