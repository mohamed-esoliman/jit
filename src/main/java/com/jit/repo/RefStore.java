package com.jit.repo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class RefStore {
    private final Repository repo;

    public RefStore(Repository repo) {
        this.repo = repo;
    }

    public String readHead() {
        try {
            return Files.readString(repo.headFile(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException("failed to read HEAD");
        }
    }

    public boolean isHeadSymbolic() {
        return readHead().startsWith("ref: ");
    }

    public String headTargetRef() {
        String v = readHead();
        if (!v.startsWith("ref: "))
            return null;
        return v.substring(5);
    }

    public String readRef(String ref) {
        try {
            Path p = repo.getJitDir().resolve(ref);
            if (!Files.exists(p))
                return null;
            return Files.readString(p, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException("failed to read ref");
        }
    }

    public void writeRef(String ref, String value) {
        try {
            Path p = repo.getJitDir().resolve(ref);
            Files.createDirectories(p.getParent());
            Files.writeString(p, value + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("failed to write ref");
        }
    }

    public void setHeadSymbolic(String ref) {
        try {
            Files.writeString(repo.headFile(), "ref: " + ref + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("failed to set HEAD");
        }
    }

    public void setHeadDetached(String hash) {
        try {
            Files.writeString(repo.headFile(), hash + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("failed to set HEAD");
        }
    }

    public String resolveHeadCommit() {
        String v = readHead();
        if (v.startsWith("ref: ")) {
            String ref = v.substring(5);
            String h = readRef(ref);
            if (h == null || h.isBlank())
                return null;
            return h;
        }
        if (v.isBlank())
            return null;
        return v;
    }
}
