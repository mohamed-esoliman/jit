package com.jit.objects;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Tree {
    public static class Entry {
        public final int mode;
        public final String name;
        public final String hashHex;

        public Entry(int mode, String name, String hashHex) {
            this.mode = mode;
            this.name = name;
            this.hashHex = hashHex;
        }
    }

    public static String store(ObjectStore store, List<Entry> entries) {
        List<Entry> copy = new ArrayList<>(entries);
        Collections.sort(copy, Comparator.comparing(e -> e.name));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Entry e : copy) {
            String header = String.format("%06o %s\0", e.mode, e.name);
            out.writeBytes(header.getBytes(StandardCharsets.UTF_8));
            byte[] bin = hexToBytes(e.hashHex);
            out.writeBytes(bin);
        }
        byte[] payload = out.toByteArray();
        return store.writeObject("tree", payload);
    }

    public static List<Entry> parse(byte[] payload) {
        List<Entry> entries = new ArrayList<>();
        int i = 0;
        while (i < payload.length) {
            int sp = indexOf(payload, (byte) ' ', i);
            int mode = Integer.parseInt(new String(payload, i, sp - i, StandardCharsets.UTF_8), 8);
            int nul = indexOf(payload, (byte) 0, sp + 1);
            String name = new String(payload, sp + 1, nul - sp - 1, StandardCharsets.UTF_8);
            byte[] bin = new byte[20];
            System.arraycopy(payload, nul + 1, bin, 0, 20);
            String hex = Hasher.toHex(bin);
            entries.add(new Entry(mode, name, hex));
            i = nul + 1 + 20;
        }
        return entries;
    }

    private static int indexOf(byte[] arr, byte target, int start) {
        for (int i = start; i < arr.length; i++)
            if (arr[i] == target)
                return i;
        return -1;
    }

    private static byte[] hexToBytes(String hex) {
        byte[] out = new byte[20];
        for (int i = 0; i < 20; i++) {
            int idx = i * 2;
            out[i] = (byte) Integer.parseInt(hex.substring(idx, idx + 2), 16);
        }
        return out;
    }
}
