package com.jit.commands;

import com.jit.repo.RefStore;
import com.jit.repo.Repository;

import java.nio.file.Paths;

public class SwitchCommand {
    public void run(String[] args) {
        if (args.length != 1)
            throw new RuntimeException("branch name required");
        String name = args[0];
        Repository repo = Repository.findRepo(Paths.get("."));
        RefStore refs = new RefStore(repo);
        String ref = "refs/heads/" + name;
        if (refs.readRef(ref) == null)
            throw new RuntimeException("branch not found");
        refs.setHeadSymbolic(ref);
    }
}
