package com.jit.commands;

import com.jit.objects.ObjectStore;
import com.jit.repo.Repository;

import java.io.IOException;
import java.nio.file.Paths;

public class CatFileCommand {
    public void run(String[] args) {
        if (args.length != 1)
            throw new RuntimeException("hash required");
        String hash = args[0];
        Repository repo = Repository.findRepo(Paths.get("."));
        ObjectStore store = new ObjectStore(repo);
        ObjectStore.StoredObject obj = store.readObject(hash);
        System.out.println(obj.type + " " + obj.payload.length);
        try {
            System.out.write(obj.payload);
        } catch (IOException e) {
            throw new RuntimeException("write failed");
        }
    }
}
