package com.jit.commands;

import com.jit.index.Index;
import com.jit.objects.ObjectStore;
import com.jit.repo.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AddCommand {
    public void run(String[] args) {
        if (args.length == 0)
            throw new RuntimeException("path required");
        Repository repo = Repository.findRepo(Paths.get("."));
        ObjectStore store = new ObjectStore(repo);
        Index index = new Index(repo, store);
        List<Path> toAdd = new ArrayList<>();
        for (String a : args)
            toAdd.add(Paths.get(a));
        List<String> ignore = readIgnore(repo.getWorkTree());
        index.addPaths(toAdd, ignore);
    }

    private List<String> readIgnore(Path root) {
        Path p = root.resolve(".jitignore");
        if (!Files.exists(p))
            return List.of();
        try {
            List<String> lines = Files.readAllLines(p);
            List<String> out = new ArrayList<>();
            for (String l : lines) {
                String t = l.trim();
                if (!t.isEmpty())
                    out.add(t);
            }
            return out;
        } catch (IOException e) {
            return List.of();
        }
    }
}
