package swift.crdt;

import swift.clocks.CausalityClock;
import swift.crdt.interfaces.CRDTUpdate;
import swift.crdt.interfaces.TxnHandle;
import swift.crdt.interfaces.TxnLocalCRDT;

public class SetIntegers extends SetVersioned<Integer, SetIntegers> {
    private static final long serialVersionUID = 1L;

    public SetIntegers() {
    }

    @Override
    protected TxnLocalCRDT<SetIntegers> getTxnLocalCopyImpl(CausalityClock versionClock, TxnHandle txn) {
        final SetIntegers creationState = isRegisteredInStore() ? null : new SetIntegers();
        SetTxnLocalInteger localView = new SetTxnLocalInteger(id, txn, versionClock, creationState,
                getValue(versionClock));
        return localView;
    }

    @Override
    protected void execute(CRDTUpdate<SetIntegers> op) {
        op.applyTo(this);
    }

    @Override
    public SetIntegers copy() {
        SetIntegers copy = new SetIntegers();
        copyLoad(copy);
        copyBase(copy);
        return copy;
    }
}