package com.jit.index;

import com.jit.objects.Blob;
import com.jit.objects.ObjectStore;
import com.jit.repo.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Index {
    public static class Entry {
        public final String path;
        public final String blob;
        public final int mode;
        public final long mtime;
        public final long size;

        public Entry(String path, String blob, int mode, long mtime, long size) {
            this.path = path;
            this.blob = blob;
            this.mode = mode;
            this.mtime = mtime;
            this.size = size;
        }
    }

    private final Repository repo;
    private final ObjectStore store;
    private final Map<String, Entry> entries = new HashMap<>();

    public Index(Repository repo, ObjectStore store) {
        this.repo = repo;
        this.store = store;
        load();
    }

    private void load() {
        Path idx = repo.indexFile();
        if (!Files.exists(idx))
            return;
        try {
            List<String> lines = Files.readAllLines(idx, StandardCharsets.UTF_8);
            for (String l : lines) {
                if (l.isBlank())
                    continue;
                String[] p = l.split("\t");
                if (p.length != 5)
                    continue;
                entries.put(p[0],
                        new Entry(p[0], p[1], Integer.parseInt(p[2]), Long.parseLong(p[3]), Long.parseLong(p[4])));
            }
        } catch (IOException e) {
            throw new RuntimeException("index read failed");
        }
    }

    private void save() {
        Path idx = repo.indexFile();
        List<String> lines = entries.values().stream()
                .sorted(Comparator.comparing(e -> e.path))
                .map(e -> String.join("\t", e.path, e.blob, Integer.toString(e.mode), Long.toString(e.mtime),
                        Long.toString(e.size)))
                .collect(Collectors.toList());
        try {
            Files.write(idx, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("index write failed");
        }
    }

    public Map<String, Entry> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public void addPaths(List<Path> paths, List<String> ignoreGlobs) {
        for (Path p : paths)
            addPath(p, ignoreGlobs);
        save();
    }

    private void addPath(Path path, List<String> ignoreGlobs) {
        Path root = repo.getWorkTree();
        Path abs = root.resolve(path).normalize();
        if (!abs.startsWith(root))
            throw new RuntimeException("path outside repo");
        if (!Files.exists(abs))
            throw new RuntimeException("path not found: " + path);
        try {
            if (Files.isDirectory(abs)) {
                Files.walkFileTree(abs, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (dir.getFileName().toString().equals(Repository.JIT_DIR))
                            return FileVisitResult.SKIP_SUBTREE;
                        String rel = root.relativize(dir).toString();
                        if (shouldIgnore(rel, ignoreGlobs))
                            return FileVisitResult.SKIP_SUBTREE;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String rel = root.relativize(file).toString();
                        if (!shouldIgnore(rel, ignoreGlobs))
                            stageFile(file, rel, attrs);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                BasicFileAttributes attrs = Files.readAttributes(abs, BasicFileAttributes.class);
                String rel = root.relativize(abs).toString();
                if (!shouldIgnore(rel, ignoreGlobs))
                    stageFile(abs, rel, attrs);
            }
        } catch (IOException e) {
            throw new RuntimeException("add failed");
        }
    }

    private boolean shouldIgnore(String rel, List<String> globs) {
        for (String g : globs) {
            if (globMatch(g, rel))
                return true;
        }
        return rel.startsWith(Repository.JIT_DIR + "/");
    }

    private boolean globMatch(String pattern, String path) {
        String p = pattern;
        if (!p.contains("*"))
            return p.equals(path);
        String[] parts = p.split("\\*", -1);
        int pos = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                if (!path.startsWith(part))
                    return false;
                pos = part.length();
            } else if (i == parts.length - 1) {
                if (!path.substring(pos).endsWith(part))
                    return false;
            } else {
                int idx = path.indexOf(part, pos);
                if (idx < 0)
                    return false;
                pos = idx + part.length();
            }
        }
        return true;
    }

    private void stageFile(Path file, String rel, BasicFileAttributes attrs) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String blob = Blob.store(store, bytes);
        int mode = 0100644;
        long mtime = attrs.lastModifiedTime().toMillis();
        long size = attrs.size();
        entries.put(rel, new Entry(rel, blob, mode, mtime, size));
    }
}
