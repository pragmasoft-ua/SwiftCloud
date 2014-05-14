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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import swift.crdt.core.CRDT;
import swift.crdt.core.CRDTIdentifier;
import sys.net.api.rpc.RpcHandle;
import sys.net.api.rpc.RpcHandler;
import sys.net.impl.KryoLib;

/**
 * Scout request to update its subscriptions.
 * 
 * @author smduarte
 */
public class UnsubscribeUpdatesRequest extends ClientRequest implements MetadataMeasure {

    protected long id;
    protected Collection<CRDTIdentifier> unsubscriptions;

    /**
     * For Kryo, do NOT use.
     */
    UnsubscribeUpdatesRequest() {
    }

    public UnsubscribeUpdatesRequest(long id, String clientId, Set<CRDTIdentifier> removals) {
        super(clientId);
        this.id = id;
        this.unsubscriptions = new ArrayList<CRDTIdentifier>(removals);
    }

    public long getId() {
        return id;
    }

    public Set<CRDTIdentifier> getUnSubscriptions() {
        return new HashSet<CRDTIdentifier>(unsubscriptions);
    }

    @Override
    public void deliverTo(RpcHandle conn, RpcHandler handler) {
        ((SwiftProtocolHandler) handler).onReceive(conn, this);
    }

    @Override
    public MetadataSizeSample getMetadataSizeSample() {
        final Kryo kryo = KryoLib.getKryoInstance();
        ByteBufferOutput buffer = new ByteBufferOutput();

        kryo.writeObject(buffer, this);
        final int totalSize = buffer.position();
        return new MetadataSizeSample(getClass().getSimpleName(), totalSize, 0, 0);
    }
}
