package com.jit.commands;

import com.jit.objects.ObjectStore;
import com.jit.repo.RefStore;
import com.jit.repo.Repository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class LogCommand {
    public void run(String[] args) {
        Repository repo = Repository.findRepo(Paths.get("."));
        ObjectStore store = new ObjectStore(repo);
        RefStore refs = new RefStore(repo);
        String head = refs.resolveHeadCommit();
        if (head == null || head.isBlank())
            return;
        String cur = head;
        while (cur != null && !cur.isBlank()) {
            ObjectStore.StoredObject obj = store.readObject(cur);
            if (!obj.type.equals("commit"))
                throw new RuntimeException("object is not commit");
            String body = new String(obj.payload, StandardCharsets.UTF_8);
            String tree = null, parent = null, author = null, timestamp = null, message = "";
            String[] lines = body.split("\n");
            int i = 0;
            for (; i < lines.length; i++) {
                String l = lines[i];
                if (l.isEmpty()) {
                    i++;
                    break;
                }
                if (l.startsWith("tree "))
                    tree = l.substring(5).trim();
                else if (l.startsWith("parent "))
                    parent = l.substring(7).trim();
                else if (l.startsWith("author "))
                    author = l.substring(7).trim();
                else if (l.startsWith("timestamp "))
                    timestamp = l.substring(10).trim();
            }
            StringBuilder msg = new StringBuilder();
            for (; i < lines.length; i++)
                msg.append(i == lines.length - 1 ? lines[i] : lines[i] + "\n");
            message = msg.toString();
            System.out.println("commit " + cur);
            System.out.println("Author: " + author);
            System.out.println("Date:   " + timestamp);
            System.out.println();
            System.out.println("    " + message.replace("\n", "\n    "));
            System.out.println();
            cur = parent;
        }
    }
}
