package swift.crdt.operations;

import swift.clocks.TripleTimestamp;
import swift.crdt.SetVersioned;

public class SetInsert<V, T extends SetVersioned<V, T>> extends BaseUpdate<T> {
    private V val;

    // required for kryo
    public SetInsert() {
    }

    public SetInsert(TripleTimestamp ts, V val) {
        super(ts);
        this.val = val;
    }

    public V getVal() {
        return this.val;
    }

    @Override
    public void applyTo(T crdt) {
        crdt.insertU(val, getTimestamp());
    }
}