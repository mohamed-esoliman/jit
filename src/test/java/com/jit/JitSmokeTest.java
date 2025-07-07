package com.jit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class JitSmokeTest {
    private static class ExecResult {
        final int code;
        final String out;
        final String err;

        ExecResult(int code, String out, String err) {
            this.code = code;
            this.out = out;
            this.err = err;
        }
    }

    private ExecResult run(Path dir, String... args) throws IOException, InterruptedException {
        String[] full = new String[args.length + 4];
        full[0] = "java";
        full[1] = "-cp";
        full[2] = Path.of("target", "classes").toAbsolutePath().toString();
        full[3] = "com.jit.cli.Jit";
        System.arraycopy(args, 0, full, 4, args.length);
        Process p = new ProcessBuilder(full).directory(dir.toFile()).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        return new ExecResult(code, out, err);
    }

    @Test
    public void endToEnd() throws Exception {
        Path temp = Files.createTempDirectory("jit-smoke");

        ExecResult r;
        r = run(temp, "init");
        assertEquals(0, r.code);
        assertTrue(Files.isDirectory(temp.resolve(".jit")));
        assertTrue(Files.exists(temp.resolve(".jit/HEAD")));

        Files.writeString(temp.resolve("a.txt"), "hello\n");
        r = run(temp, "add", "a.txt");
        assertEquals(0, r.code);
        r = run(temp, "status");
        assertEquals(0, r.code);
        assertTrue(r.out.contains("a.txt"));

        r = run(temp, "commit", "-m", "add a");
        assertEquals(0, r.code);
        String head = Files.readString(temp.resolve(".jit/refs/heads/main")).trim();
        assertTrue(head.matches("[0-9a-f]{40}"));

        r = run(temp, "log");
        assertEquals(0, r.code);
        assertTrue(r.out.contains("commit "));
        assertTrue(r.out.contains("add a"));

        r = run(temp, "branch", "feature");
        assertEquals(0, r.code);
        r = run(temp, "switch", "feature");
        assertEquals(0, r.code);
        String headFile = Files.readString(temp.resolve(".jit/HEAD"));
        assertEquals("ref: refs/heads/feature", headFile.trim());

        Files.writeString(temp.resolve("b.txt"), "b\n");
        assertEquals(0, run(temp, "add", "b.txt").code);
        assertEquals(0, run(temp, "commit", "-m", "feature: add b").code);
        r = run(temp, "log");
        assertTrue(r.out.contains("feature: add b"));

        ExecResult cat = run(temp, "cat-file", head);
        assertEquals(0, cat.code);
        String commitPayload = cat.out;
        Matcher m = Pattern.compile("^tree ([0-9a-f]{40})$", Pattern.MULTILINE).matcher(commitPayload);
        assertTrue(m.find());
        String treeHash = m.group(1);
        ExecResult ls = run(temp, "ls-tree", treeHash);
        assertEquals(0, ls.code);
        assertTrue(ls.out.contains("a.txt"));

        r = run(temp, "checkout", head);
        assertEquals(0, r.code);
        assertEquals(head, Files.readString(temp.resolve(".jit/HEAD")).trim());

        r = run(temp, "switch", "feature");
        assertEquals(0, r.code);
        r = run(temp, "checkout", "feature");
        assertEquals(0, r.code);
        assertEquals("ref: refs/heads/feature", Files.readString(temp.resolve(".jit/HEAD")).trim());
    }
}
