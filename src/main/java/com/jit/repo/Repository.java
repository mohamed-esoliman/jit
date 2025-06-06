package com.jit.repo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Repository {
    private final Path workTree;
    private final Path jitDir;
    public static final String JIT_DIR = ".jit";

    public Repository(Path workTree) {
        this.workTree = workTree.toAbsolutePath().normalize();
        this.jitDir = this.workTree.resolve(JIT_DIR);
    }

    public Path getWorkTree() {
        return workTree;
    }

    public Path getJitDir() {
        return jitDir;
    }

    public Path objectsDir() {
        return jitDir.resolve("objects");
    }

    public Path refsHeadsDir() {
        return jitDir.resolve("refs").resolve("heads");
    }

    public Path headFile() {
        return jitDir.resolve("HEAD");
    }

    public Path indexFile() {
        return jitDir.resolve("index");
    }

    public boolean isInitialized() {
        return Files.isDirectory(jitDir) && Files.isDirectory(objectsDir()) && Files.exists(headFile());
    }

    public void init() {
        try {
            if (Files.exists(jitDir))
                throw new RuntimeException("repository already exists");
            Files.createDirectories(objectsDir());
            Files.createDirectories(refsHeadsDir());
            Files.writeString(headFile(), "ref: refs/heads/main\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            Files.writeString(refsHeadsDir().resolve("main"), "", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            if (!Files.exists(indexFile()))
                Files.write(indexFile(), new byte[0], StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("init failed");
        }
    }

    public static Repository findRepo(Path start) {
        Path p = start.toAbsolutePath().normalize();
        while (p != null) {
            if (Files.isDirectory(p.resolve(JIT_DIR)))
                return new Repository(p);
            p = p.getParent();
        }
        throw new RuntimeException("not a jit repository");
    }
}
