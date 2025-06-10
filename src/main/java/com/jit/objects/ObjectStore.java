package com.jit.objects;

import com.jit.repo.Repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ObjectStore {
    private final Repository repo;

    public ObjectStore(Repository repo) {
        this.repo = repo;
    }

    public String writeObject(String type, byte[] payload) {
        byte[] header = (type + " " + payload.length + "\n").getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[header.length + payload.length];
        System.arraycopy(header, 0, all, 0, header.length);
        System.arraycopy(payload, 0, all, header.length, payload.length);
        String hex = Hasher.toHex(Hasher.sha1(all));
        Path p = pathFor(hex);
        try {
            if (!Files.exists(p)) {
                Files.createDirectories(p.getParent());
                Files.write(p, all, StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            throw new RuntimeException("object write failed");
        }
        return hex;
    }

    public StoredObject readObject(String hash) {
        try {
            byte[] all = Files.readAllBytes(pathFor(hash));
            int i = indexOf(all, (byte) '\n');
            if (i < 0)
                throw new RuntimeException("corrupt object");
            String header = new String(all, 0, i, StandardCharsets.UTF_8);
            String[] parts = header.split(" ");
            if (parts.length != 2)
                throw new RuntimeException("corrupt object");
            String type = parts[0];
            int size = Integer.parseInt(parts[1]);
            byte[] payload = new byte[size];
            System.arraycopy(all, i + 1, payload, 0, size);
            return new StoredObject(type, payload);
        } catch (IOException e) {
            throw new RuntimeException("object not found");
        }
    }

    private int indexOf(byte[] bytes, byte target) {
        for (int i = 0; i < bytes.length; i++)
            if (bytes[i] == target)
                return i;
        return -1;
    }

    private Path pathFor(String hash) {
        String dir = hash.substring(0, 2);
        String file = hash.substring(2);
        return repo.objectsDir().resolve(dir).resolve(file);
    }

    public static class StoredObject {
        public final String type;
        public final byte[] payload;

        public StoredObject(String type, byte[] payload) {
            this.type = type;
            this.payload = payload;
        }
    }
}
