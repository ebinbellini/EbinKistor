package ebinbellini.ebinkistor.check;

public interface LockableChest {
    boolean isLocked();
    void setLockingPlayer(String id, String name);
    String getLockOwner();
    String getLockID();
}
