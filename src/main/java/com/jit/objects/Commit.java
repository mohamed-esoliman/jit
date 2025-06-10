package com.jit.objects;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Commit {
    public final String tree;
    public final String parent;
    public final String authorName;
    public final String authorEmail;
    public final String timestamp;
    public final String message;

    public Commit(String tree, String parent, String authorName, String authorEmail, String timestamp, String message) {
        this.tree = tree;
        this.parent = parent;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.timestamp = timestamp;
        this.message = message;
    }

    public static String nowIso() {
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now());
    }

    public byte[] serializePayload() {
        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(tree).append("\n");
        if (parent != null && !parent.isBlank())
            sb.append("parent ").append(parent).append("\n");
        sb.append("author ").append(authorName).append(" ").append(authorEmail).append("\n");
        sb.append("timestamp ").append(timestamp).append("\n");
        sb.append("\n");
        sb.append(message).append("\n");
        String body = sb.toString().replace("\r\n", "\n").replace("\r", "\n");
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
