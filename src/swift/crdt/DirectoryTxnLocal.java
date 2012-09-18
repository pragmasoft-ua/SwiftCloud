package swift.crdt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import swift.clocks.CausalityClock;
import swift.clocks.TripleTimestamp;
import swift.crdt.interfaces.CRDT;
import swift.crdt.interfaces.CRDTQuery;
import swift.crdt.interfaces.TxnHandle;
import swift.crdt.interfaces.TxnLocalCRDT;
import swift.crdt.operations.DirectoryCreateUpdate;
import swift.crdt.operations.DirectoryRemoveUpdate;
import swift.exceptions.NetworkException;
import swift.exceptions.NoSuchObjectException;
import swift.exceptions.VersionNotFoundException;
import swift.exceptions.WrongTypeException;
import swift.utils.Pair;

/**
 * Transaction-local view of Directory CRDT.
 * 
 * @author annettebieniusa
 */

/*
 * Some information on dynamic type checking in Java for compliance of
 * identifiers and CRDT types:
 * 
 * To obtain the fully qualified name of a class: val.getClass().getName()
 * V.getName()
 * 
 * To reconstruct the class and type-safe casts: w = Class.forName(FQN) assert(w
 * == v) v.cast(object)
 */

public class DirectoryTxnLocal extends BaseCRDTTxnLocal<DirectoryVersioned> {
    private static String dirTable = "DIR";
    private Map<CRDTIdentifier, Set<TripleTimestamp>> dir;

    public DirectoryTxnLocal(CRDTIdentifier id, TxnHandle txn, CausalityClock clock, DirectoryVersioned creationState,
            Map<CRDTIdentifier, Set<TripleTimestamp>> payload) {
        super(id, txn, clock, creationState);
        this.dir = payload;
    }

    public static String getClassName(CRDTIdentifier id) {
        return id.getKey().split(":")[1];
    }

    public static String getFullPath(CRDTIdentifier id) {
        return id.getKey().split(":")[0];
    }

    public static String getEntryName(CRDTIdentifier id) {
        String path = getFullPath(id);
        String[] splitted = path.split("/");
        return splitted[splitted.length - 1];
    }

    /**
     * Table under which all file system entries are stored.
     * <p>
     * FIXME Can we be there more flexible? Hard-coding this name is just quick
     * fix.
     */
    public static String getDirTable() {
        return dirTable;
    }

    public static <V extends CRDT<V>> String getDirEntry(String name, Class<V> c) {
        return name + ":" + c.getName();
    }

    public static <V extends CRDT<V>> CRDTIdentifier getCRDTIdentifier(String fullDirName, String name, Class<V> c) {
        String prefix = (fullDirName == "/" ? "" : fullDirName + "/");
        return new CRDTIdentifier(DirectoryTxnLocal.getDirTable(), prefix + getDirEntry(name, c));
    }

    public static <V extends CRDT<V>> CRDTIdentifier createRootId(String key, Class<V> c) {
        CRDTIdentifier newCRDTId = getCRDTIdentifier("", key, c);
        return newCRDTId;

    }

    public <V extends CRDT<V>> CRDTIdentifier createNewEntry(String key, Class<V> c) throws WrongTypeException,
            NoSuchObjectException, VersionNotFoundException, NetworkException {
        // implemented as remove followed by add
        TripleTimestamp ts = nextTimestamp();
        Set<TripleTimestamp> tss = new HashSet<TripleTimestamp>();
        tss.add(ts);
        CRDTIdentifier newCRDTId = getCRDTIdentifier(getFullPath(this.id), key, c);
        dir.put(newCRDTId, tss);
        this.getTxnHandle().get(newCRDTId, true, c);
        registerLocalOperation(new DirectoryCreateUpdate(newCRDTId, ts));

        // Reconstruct the path to the root for implementing the
        // add-/update-wins semantics
        String[] parentDirs = getFullPath(this.id).split("/");
        String name = parentDirs[parentDirs.length - 1];
        String path = "";
        for (int i = 1; i < parentDirs.length - 1; i++) {
            path = path.concat("/");
            path = path.concat(parentDirs[i]);
        }

        if (!path.equals("")) {
            CRDTIdentifier parentId = new CRDTIdentifier(DirectoryTxnLocal.getDirTable(), getDirEntry(path,
                    DirectoryVersioned.class));
            DirectoryTxnLocal parent = this.getTxnHandle().get(parentId, false, DirectoryVersioned.class);
            parent.createNewEntry(name, DirectoryVersioned.class);
        }
        return newCRDTId;
    }

    public <V extends CRDT<V>> void removeEntry(String key, Class<V> c) throws WrongTypeException,
            NoSuchObjectException, VersionNotFoundException, NetworkException, ClassNotFoundException {
        CRDTIdentifier id = getCRDTIdentifier(getFullPath(this.id), key, c);
        Set<TripleTimestamp> toBeRemoved = dir.remove(id);
        if (toBeRemoved != null) {
            TripleTimestamp ts = nextTimestamp();
            registerLocalOperation(new DirectoryRemoveUpdate(getCRDTIdentifier(getFullPath(this.id), key, c),
                    toBeRemoved, ts));
        }
        if (c.equals(DirectoryVersioned.class)) {
            DirectoryTxnLocal child = (DirectoryTxnLocal) this.getTxnHandle().get(id, false, c);
            child.deleteDirectoryRecursively(c);
        }
    }

    private <V extends CRDT<V>> void deleteDirectoryRecursively(Class<V> c) throws ClassNotFoundException,
            WrongTypeException, NoSuchObjectException, VersionNotFoundException, NetworkException {
        Set<CRDTIdentifier> currentEntries = new HashSet<CRDTIdentifier>(this.dir.keySet());
        for (CRDTIdentifier e : currentEntries) {
            removeEntry(getEntryName(e), (Class<V>) Class.forName(getClassName(e)));
        }
    }

    public <V extends CRDT<V>, L extends TxnLocalCRDT<V>> L get(String key, Class<V> c) throws WrongTypeException,
            NoSuchObjectException, VersionNotFoundException, NetworkException {
        CRDTIdentifier entry = getCRDTIdentifier(getFullPath(this.id), key, c);
        if (dir.keySet().contains(entry)) {
            return this.getTxnHandle().get(entry, false, c, null);
        } else {
            return null;
        }
    }

    public <V extends CRDT<V>> boolean contains(String key, Class<V> c) {
        return dir.containsKey(getCRDTIdentifier(getFullPath(this.id), key, c));
    }

    @Override
    public Object executeQuery(CRDTQuery<DirectoryVersioned> query) {
        return query.executeAt(this);
    }

    @Override
    public Collection<Pair<String, Class<?>>> getValue() {
        Collection<Pair<String, Class<?>>> dirEntries = new HashSet<Pair<String, Class<?>>>();
        for (CRDTIdentifier id : dir.keySet()) {
            try {
                String name = getEntryName(id);
                String clss = getClassName(id);
                Pair<String, Class<?>> entry = new Pair<String, Class<?>>(name, Class.forName(clss));
                dirEntries.add(entry);
            } catch (ClassNotFoundException e) {
                System.err.println("This class is unkown :" + id.getKey());
                e.printStackTrace();
            }
        }
        return dirEntries;
    }

}