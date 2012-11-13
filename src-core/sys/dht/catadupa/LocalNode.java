package sys.dht.catadupa;

import static sys.net.api.Networking.Networking;
import sys.RpcServices;
import sys.dht.catadupa.msgs.CatadupaHandler;
import sys.net.api.Endpoint;
import sys.net.api.Networking.TransportProvider;
import sys.net.api.rpc.RpcEndpoint;
import sys.net.api.rpc.RpcFactory;

import static sys.Sys.*;

/**
 * 
 * @author smd
 * 
 */
public class LocalNode extends CatadupaHandler {

	protected Node self;
	protected RpcEndpoint rpc;
	protected Endpoint endpoint;
	protected RpcFactory rpcFactory;

	LocalNode() {
		initLocalNode();
	}

	public void initLocalNode() {
		rpcFactory = Networking.rpcBind(0, TransportProvider.DEFAULT);
		rpc = rpcFactory.toService(RpcServices.CATADUPA.ordinal(), this);
		self = new Node(rpc.localEndpoint(), Sys.getDatacenter());
		SeedDB.init(self);
	}
}