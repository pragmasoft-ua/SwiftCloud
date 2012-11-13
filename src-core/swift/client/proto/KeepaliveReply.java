package swift.client.proto;

import sys.net.api.rpc.RpcHandle;
import sys.net.api.rpc.RpcHandler;
import sys.net.api.rpc.RpcMessage;

/**
 * Server reply to client keepalive message.
 * 
 * @author mzawirski
 */
public class KeepaliveReply implements RpcMessage {
    protected boolean timestampRenewed;
    protected boolean versionAvailable;
    protected long validityMillis;

    KeepaliveReply() {
    }

    public KeepaliveReply(boolean timestampRenewed, boolean versionAvailable, long validityMillis) {
        this.timestampRenewed = timestampRenewed;
        this.versionAvailable = versionAvailable;
        this.validityMillis = validityMillis;
    }

    /**
     * @return true if timestamp validity renewal was requested and successfully
     *         renewed
     */
    public boolean isTimestampRenewed() {
        return timestampRenewed;
    }

    /**
     * @return true if version keepalive was requested and all objects are still
     *         available in this version
     */
    public boolean isVersionAvailable() {
        return versionAvailable;
    }

    /**
     * @return until what time the timestamp & objects stay available unless
     *         extended using keepalive; specified in milliseconds since the
     *         UNIX epoch
     */
    public long getValidityMillis() {
        return validityMillis;
    }

    @Override
    public void deliverTo(RpcHandle conn, RpcHandler handler) {
        ((KeepaliveReplyHandler) handler).onReceive(conn, this);
    }
}