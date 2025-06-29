package com.jit.commands;

import com.jit.objects.ObjectStore;
import com.jit.objects.Tree;
import com.jit.repo.Repository;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

public class LsTreeCommand {
    public void run(String[] args) {
        if (args.length != 1)
            throw new RuntimeException("hash required");
        String hash = args[0];
        Repository repo = Repository.findRepo(Paths.get("."));
        ObjectStore store = new ObjectStore(repo);
        ObjectStore.StoredObject obj = store.readObject(hash);
        if (!obj.type.equals("tree"))
            throw new RuntimeException("not a tree");
        List<Tree.Entry> entries = Tree.parse(obj.payload);
        entries.sort(Comparator.comparing(e -> e.name));
        for (Tree.Entry e : entries) {
            String type = e.mode == 040000 ? "tree" : "blob";
            System.out.println(String.format("%06o %s %s\t%s", e.mode, type, e.hashHex, e.name));
        }
    }
}
