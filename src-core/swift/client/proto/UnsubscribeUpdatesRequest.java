package swift.client.proto;

import swift.crdt.CRDTIdentifier;
import sys.net.api.rpc.RpcHandle;
import sys.net.api.rpc.RpcHandler;

/**
 * Client request to unsubscribe update notifications for an object.
 * 
 * @author mzawirski
 */
public class UnsubscribeUpdatesRequest extends ClientRequest {
    protected CRDTIdentifier uid;

    /**
     * Fake constructor for Kryo serialization. Do NOT use.
     */
    UnsubscribeUpdatesRequest() {
    }

    public UnsubscribeUpdatesRequest(String clientId, CRDTIdentifier uid) {
        super(clientId);
        this.uid = uid;
    }

    /**
     * @return object id to unsubscribe
     */
    public CRDTIdentifier getUid() {
        return uid;
    }

    @Override
    public void deliverTo(RpcHandle conn, RpcHandler handler) {
        ((SwiftServer) handler).onReceive(conn, this);
    }
}