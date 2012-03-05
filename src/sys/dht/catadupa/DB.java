package sys.dht.catadupa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import sys.dht.catadupa.crdts.CRDTRuntime;
import sys.dht.catadupa.crdts.ORSet;
import sys.dht.catadupa.crdts.time.LVV;
import sys.dht.catadupa.crdts.time.Timestamp;
import sys.dht.catadupa.msgs.CatadupaCastPayload;
import sys.dht.catadupa.msgs.DbMergeReply;

import static sys.Sys.*;
import static sys.utils.Log.Log;

import static sys.dht.catadupa.Config.*;

/**
 * 
 * @author smd
 * 
 */
public class DB {

	final long KEY_RANGE = 1L << Config.NODE_KEY_LENGTH;
	final long MAX_KEY = (1L << Config.NODE_KEY_LENGTH) - 1L;
	final long SLICE_RANDOM_OFFSET = new Random(100).nextLong() >>> 10;

	CRDTRuntime rt;
	final Node self;
	final CatadupaNode owner;
	boolean joined = false;
	ORSet<MembershipUpdate> membership;
	SortedMap<Long, Node> k2n = new TreeMap<Long, Node>();

	public DB(CatadupaNode owner) {
		this.owner = owner;
		this.self = owner.self;
		this.rt = new CRDTRuntime("" + self.key);
		this.membership = new ORSet<MembershipUpdate>();
		this.membership.setUpdatesRecorder(rt);

		for (Node i : SeedDB.nodes())
			k2n.put(i.key, i);
	}

	// public void merge( DbMergeReply r ) {
	// if( r.delta != null ) {
	// clock().merge( r.clock ) ;
	//
	// Collection<MembershipUpdate> added = new ArrayList<MembershipUpdate>(),
	// removed = new ArrayList<MembershipUpdate>();
	// membership.merge( r.delta, added, removed ) ;
	// for( MembershipUpdate i : added )
	// mergeNodes( i) ;
	// }
	// }

	synchronized public void merge(DbMergeReply r) {
		if (r.delta != null) {
			clock().merge(r.clock);

			Collection<MembershipUpdate> added = new ArrayList<MembershipUpdate>(), removed = new ArrayList<MembershipUpdate>();
			membership.merge(r.toORSet(), added, removed);
			for (MembershipUpdate i : added)
				mergeNodes(i);
		}
	}

	synchronized public Timestamp merge(MembershipUpdate m) {
		Timestamp ts = rt.recordUpdate(membership);
		membership.add(m, ts);
		return ts;
	}

	synchronized public void merge(CatadupaCastPayload m) {
		membership.add(m.data, m.timestamp);
		mergeNodes(m.data);
	}

	synchronized void mergeNodes(MembershipUpdate m) {
		for (Node i : m.arrivals) {
			Node old = k2n.put(i.key, i);

			if (!joined) {
				joined |= i.key == self.key;
				if (joined) {
					Log.fine("Joined Catadupa....");
				}
			}
			if (old == null || (old.key == i.key && old.isOffline())) {
				owner.onNodeAdded(i);
			}
		}

		for (Node i : m.departures) {
			Node old = k2n.put(i.key, new DeadNode(i));
			if (old == null || (old.key == i.key && old.isOnline())) {
				owner.onNodeRemoved(i);
			}
		}
	}

	public LVV clock() {
		return rt.getCausalityClock();
	}

	// -------------------------------------------------------------------------------
	Node randomNode() {
		long key = new java.math.BigInteger(Config.NODE_KEY_LENGTH, Sys.rg).longValue() & MAX_KEY;

		for (Node i : nodes(key))
			if (i.key != self.key)
				return i;

		return null;
	}

	// -------------------------------------------------------------------------------
	public Node aggregatorFor(long key) {
		return aggregatorFor(true, key);
	}

	private Node aggregatorFor(boolean excludeSelf, long key) {
		return aggregatorFor(0, excludeSelf, key);
	}

	// -------------------------------------------------------------------------------
	public Node aggregatorFor(int level, long key) {
		return aggregatorFor(level, true, key);
	}

	private Node aggregatorFor(int level, boolean excludeSelf, long key) {
		long sliceWidth = KEY_RANGE / (1 << level);

		long levelOffset = ((level + 1) * SLICE_RANDOM_OFFSET) & MAX_KEY;

		long slice = ((key - levelOffset) & MAX_KEY) / sliceWidth;

		long sliceLKey = (slice * sliceWidth + levelOffset) & MAX_KEY;
		long sliceHKey = (sliceLKey + sliceWidth - 1L) & MAX_KEY;

		// System.out.printf(
		// "level:%s sliceWidth:%s offset: %s key: %s -> slice:%s-->[%s -> %s]\n",
		// level, sliceWidth, levelOffset, key, slice, sliceLKey, sliceHKey );

		for (Node candidate : nodes(sliceLKey, sliceHKey))
			if (excludeSelf && candidate.key == self.key)
				continue;
			else
				return candidate;
		return self;
	}

	// -------------------------------------------------------------------------------
	public Iterable<Node> nodes(long L, long H) {
		try {
			if (L <= H)
				return k2n.subMap(L, H + 1L).values();
			else {
				Iterator<Node> first = k2n.tailMap(L).values().iterator();
				Iterator<Node> second = k2n.headMap(H).values().iterator();
				return new AppendIterator<Node>(first, second);
			}
		} catch (Exception x) {
			System.out.println(L + "/" + H);
			throw new RuntimeException(x.getMessage());
		}
	}

	public Iterable<Node> nodes(long key) {
		Iterator<Node> first = k2n.tailMap(key).values().iterator();
		Iterator<Node> second = k2n.headMap(key).values().iterator();
		return new AppendIterator<Node>(first, second);
	}
}

class AppendIterator<T> implements Iterator<T>, Iterable<T> {

	Iterator<T> curr, first, second;

	AppendIterator(Iterator<T> a, Iterator<T> b) {
		curr = first = a;
		second = b;
	}

	@Override
	public boolean hasNext() {
		if (curr.hasNext())
			return true;
		else
			return (curr = second).hasNext();
	}

	@Override
	public T next() {
		return curr.next();
	}

	@Override
	public void remove() {
		curr.remove();
	}

	public Iterator<T> iterator() {
		return this;
	}
}