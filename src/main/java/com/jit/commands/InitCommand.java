package com.jit.commands;

import com.jit.repo.Repository;

import java.nio.file.Paths;

public class InitCommand {
    public void run(String[] args) {
        Repository repo = new Repository(Paths.get("."));
        repo.init();
        System.out.println("Initialized empty Jit repository");
    }
}
