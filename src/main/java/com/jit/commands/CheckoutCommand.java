package com.jit.commands;

import com.jit.index.Index;
import com.jit.objects.ObjectStore;
import com.jit.objects.Tree;
import com.jit.repo.RefStore;
import com.jit.repo.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class CheckoutCommand {
    public void run(String[] args) {
        if (args.length < 1)
            throw new RuntimeException("target required");
        boolean force = false;
        List<String> rest = new ArrayList<>();
        for (String a : args) {
            if (a.equals("--force"))
                force = true;
            else
                rest.add(a);
        }
        if (rest.isEmpty())
            throw new RuntimeException("target required");
        String target = rest.get(0);

        Repository repo = Repository.findRepo(Paths.get("."));
        ObjectStore store = new ObjectStore(repo);
        RefStore refs = new RefStore(repo);

        String hash = null;
        String ref = "refs/heads/" + target;
        String refVal = refs.readRef(ref);
        boolean isBranch = false;
        if (refVal != null) {
            hash = refVal;
            isBranch = true;
        } else
            hash = target;
        if (hash == null || hash.isBlank())
            throw new RuntimeException("unknown target");
        if (hash.length() != 40)
            throw new RuntimeException("invalid hash or unknown branch");

        ObjectStore.StoredObject co = store.readObject(hash);
        if (!co.type.equals("commit"))
            throw new RuntimeException("target not a commit");
        String body = new String(co.payload, StandardCharsets.UTF_8);
        String treeHash = null;
        for (String l : body.split("\n")) {
            if (l.startsWith("tree ")) {
                treeHash = l.substring(5).trim();
                break;
            }
        }
        if (treeHash == null)
            throw new RuntimeException("commit missing tree");

        Map<String, Tree.Entry> targetEntries = new HashMap<>();
        collectTree(store, treeHash, "", targetEntries);

        Index index = new Index(repo, store);
        if (!force) {
            for (var e : index.entries().entrySet()) {
                String path = e.getKey();
                Path abs = repo.getWorkTree().resolve(path);
                if (Files.exists(abs)) {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(abs, BasicFileAttributes.class);
                        if (attrs.size() != e.getValue().size
                                || attrs.lastModifiedTime().toMillis() != e.getValue().mtime) {
                            byte[] bytes = Files.readAllBytes(abs);
                            String blob = store.writeObject("blob", bytes);
                            Tree.Entry te = targetEntries.get(path);
                            if (te == null || !te.hashHex.equals(blob))
                                throw new RuntimeException("local changes would be overwritten");
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException("checkout failed");
                    }
                }
            }
        }

        try {
            Set<String> keepDirs = new HashSet<>();
            for (String p : targetEntries.keySet()) {
                Path abs = repo.getWorkTree().resolve(p).getParent();
                if (abs != null)
                    keepDirs.add(abs.toString());
            }
            if (refs.resolveHeadCommit() != null) {
                Map<String, String> current = new HashMap<>();
                String curTreeHash = null;
                ObjectStore.StoredObject ch = store.readObject(refs.resolveHeadCommit());
                if (ch.type.equals("commit")) {
                    String cb = new String(ch.payload, StandardCharsets.UTF_8);
                    for (String l : cb.split("\n"))
                        if (l.startsWith("tree ")) {
                            curTreeHash = l.substring(5).trim();
                            break;
                        }
                }
                if (curTreeHash != null)
                    collectTreeBlobs(store, curTreeHash, "", current);
                for (String path : current.keySet()) {
                    if (!targetEntries.containsKey(path)) {
                        Path abs = repo.getWorkTree().resolve(path);
                        Files.deleteIfExists(abs);
                    }
                }
            }
            for (var e : targetEntries.entrySet()) {
                Path abs = repo.getWorkTree().resolve(e.getKey());
                Files.createDirectories(abs.getParent());
                ObjectStore.StoredObject bo = store.readObject(e.getValue().hashHex);
                if (!bo.type.equals("blob"))
                    throw new RuntimeException("tree entry not blob");
                Files.write(abs, bo.payload, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException ex) {
            throw new RuntimeException("checkout failed");
        }

        if (isBranch)
            refs.setHeadSymbolic(ref);
        else
            refs.setHeadDetached(hash);

        Index newIndex = new Index(repo, store);
        Map<String, Index.Entry> map = new HashMap<>();
        for (var e : targetEntries.entrySet()) {
            Path abs = repo.getWorkTree().resolve(e.getKey());
            try {
                BasicFileAttributes attrs = Files.readAttributes(abs, BasicFileAttributes.class);
                map.put(e.getKey(), new Index.Entry(e.getKey(), e.getValue().hashHex, 0100644,
                        attrs.lastModifiedTime().toMillis(), attrs.size()));
            } catch (IOException ex) {
                throw new RuntimeException("index update failed");
            }
        }
        try {
            Path idx = repo.indexFile();
            List<String> lines = new ArrayList<>();
            for (var e : new TreeMap<>(map).values()) {
                lines.add(String.join("\t", e.path, e.blob, Integer.toString(e.mode), Long.toString(e.mtime),
                        Long.toString(e.size)));
            }
            Files.write(idx, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new RuntimeException("index write failed");
        }
    }

    private void collectTree(ObjectStore store, String treeHash, String prefix, Map<String, Tree.Entry> out) {
        ObjectStore.StoredObject to = store.readObject(treeHash);
        if (!to.type.equals("tree"))
            throw new RuntimeException("expected tree");
        List<Tree.Entry> entries = Tree.parse(to.payload);
        for (Tree.Entry e : entries) {
            String name = prefix.isEmpty() ? e.name : prefix + "/" + e.name;
            if (e.mode == 0100644)
                out.put(name, e);
            else if (e.mode == 040000)
                collectTree(store, e.hashHex, name, out);
        }
    }

    private void collectTreeBlobs(ObjectStore store, String treeHash, String prefix, Map<String, String> out) {
        ObjectStore.StoredObject to = store.readObject(treeHash);
        List<Tree.Entry> entries = Tree.parse(to.payload);
        for (Tree.Entry e : entries) {
            String name = prefix.isEmpty() ? e.name : prefix + "/" + e.name;
            if (e.mode == 0100644)
                out.put(name, e.hashHex);
            else if (e.mode == 040000)
                collectTreeBlobs(store, e.hashHex, name, out);
        }
    }
}
