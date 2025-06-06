package com.jit.repo;

public class Head {
    private final RefStore refs;

    public Head(RefStore refs) {
        this.refs = refs;
    }

    public boolean isSymbolic() {
        return refs.isHeadSymbolic();
    }

    public String targetRef() {
        return refs.headTargetRef();
    }

    public String currentCommit() {
        return refs.resolveHeadCommit();
    }
}
