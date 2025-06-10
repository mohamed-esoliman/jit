package com.jit.objects;

public class Blob {
    public static String store(ObjectStore store, byte[] content) {
        return store.writeObject("blob", content);
    }
}
