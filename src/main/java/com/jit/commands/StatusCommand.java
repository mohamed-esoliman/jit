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

public class StatusCommand {
    public void run(String[] args) {
        Repository repo = Repository.findRepo(Paths.get("."));
        ObjectStore store = new ObjectStore(repo);
        Index index = new Index(repo, store);
        RefStore refs = new RefStore(repo);
        String head = refs.resolveHeadCommit();
        Map<String, Index.Entry> idx = new TreeMap<>(index.entries());

        Map<String, String> headBlobs = new HashMap<>();
        if (head != null) {
            ObjectStore.StoredObject co = store.readObject(head);
            if (!co.type.equals("commit"))
                throw new RuntimeException("HEAD not commit");
            String commitText = new String(co.payload, StandardCharsets.UTF_8);
            String treeHash = null;
            for (String l : commitText.split("\n")) {
                if (l.startsWith("tree ")) {
                    treeHash = l.substring(5).trim();
                    break;
                }
            }
            if (treeHash != null)
                collectTreeBlobs(store, treeHash, "", headBlobs);
        }

        List<String> staged = new ArrayList<>();
        for (var e : idx.entrySet()) {
            String path = e.getKey();
            String headBlob = headBlobs.get(path);
            if (headBlob == null || !headBlob.equals(e.getValue().blob))
                staged.add(path);
        }

        List<String> modified = new ArrayList<>();
        List<String> untracked = new ArrayList<>();
        Path root = repo.getWorkTree();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(repo.getJitDir()))
                        return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relp = root.relativize(file);
                    String rel = relp.toString();
                    Index.Entry ie = idx.get(rel);
                    if (ie == null) {
                        if (!rel.startsWith(Repository.JIT_DIR + "/"))
                            untracked.add(rel);
                    } else {
                        if (ie.size != attrs.size() || ie.mtime != attrs.lastModifiedTime().toMillis()) {
                            byte[] bytes = Files.readAllBytes(file);
                            String blob = store.writeObject("blob", bytes);
                            if (!blob.equals(ie.blob))
                                modified.add(rel);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("status failed");
        }

        System.out.println("Changes to be committed:");
        for (String s : staged)
            System.out.println("  " + s);
        System.out.println();
        System.out.println("Changes not staged for commit:");
        for (String s : modified)
            System.out.println("  " + s);
        System.out.println();
        System.out.println("Untracked files:");
        for (String s : untracked)
            System.out.println("  " + s);
    }

    private void collectTreeBlobs(ObjectStore store, String treeHash, String prefix, Map<String, String> out) {
        ObjectStore.StoredObject to = store.readObject(treeHash);
        if (!to.type.equals("tree"))
            throw new RuntimeException("expected tree");
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
