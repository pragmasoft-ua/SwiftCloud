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
import swift.clocks.Timestamp;
import sys.net.api.rpc.RpcHandle;
import sys.net.api.rpc.RpcHandler;

/**
 * Server request to generate a timestamp for a transaction.
 * 
 * @author nmp
 */
public class GenerateDCTimestampRequest extends ClientRequest {
    Timestamp cltTimestamp;
    CausalityClock dependencyClk;

    // long cltDependency;

    // Fake constructor for Kryo serialization. Do NOT use.
    GenerateDCTimestampRequest() {
    }

    public GenerateDCTimestampRequest(String clientId, boolean disasterSafeSession, Timestamp cltTimestamp,
            CausalityClock dependencyClk) {
        super(clientId, disasterSafeSession);
        this.cltTimestamp = cltTimestamp;
        this.dependencyClk = dependencyClk;
        // cltDependency = dependencyClk.getLatestCounter(clientId);
        dependencyClk.drop(clientId);
    }

    /**
     * @return client timestamp
     */
    public Timestamp getCltTimestamp() {
        return cltTimestamp;
    }

    @Override
    public void deliverTo(RpcHandle conn, RpcHandler handler) {
        ((SwiftProtocolHandler) handler).onReceive(conn, this);
    }

    public CausalityClock getDependencyClk() {
        return dependencyClk;
    }
}
