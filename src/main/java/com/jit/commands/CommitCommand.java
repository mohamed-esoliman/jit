package com.jit.commands;

import com.jit.index.Index;
import com.jit.objects.Commit;
import com.jit.objects.ObjectStore;
import com.jit.objects.Tree;
import com.jit.repo.RefStore;
import com.jit.repo.Repository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

public class CommitCommand {
    public void run(String[] args) {
        String message = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-m") && i + 1 < args.length) {
                message = args[i + 1];
                i++;
            }
        }
        if (message == null)
            throw new RuntimeException("missing -m message");
        Repository repo = Repository.findRepo(Paths.get("."));
        ObjectStore store = new ObjectStore(repo);
        Index index = new Index(repo, store);
        if (index.entries().isEmpty())
            throw new RuntimeException("nothing to commit");

        Map<String, Index.Entry> entries = new TreeMap<>(index.entries());
        String rootTree = writeTrees(store, entries);

        RefStore refs = new RefStore(repo);
        String parent = refs.resolveHeadCommit();
        String authorName = System.getenv().getOrDefault("JIT_AUTHOR_NAME", "User");
        String authorEmail = System.getenv().getOrDefault("JIT_AUTHOR_EMAIL", "user@example.com");
        Commit commit = new Commit(rootTree, parent, authorName, authorEmail, Commit.nowIso(), message);
        byte[] payload = commit.serializePayload();
        String hash = store.writeObject("commit", payload);

        if (refs.isHeadSymbolic()) {
            String ref = refs.headTargetRef();
            if (ref == null)
                throw new RuntimeException("HEAD invalid");
            refs.writeRef(ref, hash);
        } else {
            refs.setHeadDetached(hash);
        }
        System.out.println("Committed " + hash);
    }

    private String writeTrees(ObjectStore store, Map<String, Index.Entry> entries) {
        Map<String, String> fileToBlob = new TreeMap<>();
        for (var e : entries.entrySet())
            fileToBlob.put(e.getKey(), e.getValue().blob);
        Map<String, String> dirToHash = new HashMap<>();
        return buildDirTree(store, "", fileToBlob, dirToHash);
    }

    private String buildDirTree(ObjectStore store, String dir, Map<String, String> fileToBlob,
            Map<String, String> cache) {
        if (cache.containsKey(dir))
            return cache.get(dir);
        List<Tree.Entry> list = new ArrayList<>();
        int dirLen = dir.isEmpty() ? 0 : dir.length() + 1;
        Set<String> childDirs = new TreeSet<>();
        for (String path : fileToBlob.keySet()) {
            if (dir.isEmpty() ? !path.contains("/") : path.startsWith(dir + "/")) {
                String rest = dir.isEmpty() ? path : path.substring(dirLen);
                int slash = rest.indexOf('/');
                if (slash < 0) {
                    String name = rest;
                    list.add(new Tree.Entry(0100644, name, fileToBlob.get(path)));
                } else {
                    String child = rest.substring(0, slash);
                    childDirs.add(child);
                }
            }
        }
        for (String child : childDirs) {
            String childPath = dir.isEmpty() ? child : dir + "/" + child;
            String childHash = buildDirTree(store, childPath, fileToBlob, cache);
            list.add(new Tree.Entry(040000, child, childHash));
        }
        String hash = Tree.store(store, list);
        cache.put(dir, hash);
        return hash;
    }
}
