package zzk.townshipscheduler.backend.tfdemo.projectscheduling;

public class GlobalResource
        extends Resource {

    public GlobalResource() {
    }

    public GlobalResource(String id, int capacity) {
        super(id, capacity);
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public boolean isRenewable() {
        return true;
    }

}
