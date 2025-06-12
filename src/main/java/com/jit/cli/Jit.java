package com.jit.cli;

import com.jit.commands.AddCommand;
import com.jit.commands.BranchCommand;
import com.jit.commands.CatFileCommand;
import com.jit.commands.CheckoutCommand;
import com.jit.commands.CommitCommand;
import com.jit.commands.InitCommand;
import com.jit.commands.LogCommand;
import com.jit.commands.LsTreeCommand;
import com.jit.commands.StatusCommand;
import com.jit.commands.SwitchCommand;

public class Jit {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: jit <command> [<args>]");
            System.exit(1);
        }
        String cmd = args[0];
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        try {
            switch (cmd) {
                case "init":
                    new InitCommand().run(rest);
                    break;
                case "add":
                    new AddCommand().run(rest);
                    break;
                case "status":
                    new StatusCommand().run(rest);
                    break;
                case "commit":
                    new CommitCommand().run(rest);
                    break;
                case "log":
                    new LogCommand().run(rest);
                    break;
                case "checkout":
                    new CheckoutCommand().run(rest);
                    break;
                case "branch":
                    new BranchCommand().run(rest);
                    break;
                case "switch":
                    new SwitchCommand().run(rest);
                    break;
                case "cat-file":
                    new CatFileCommand().run(rest);
                    break;
                case "ls-tree":
                    new LsTreeCommand().run(rest);
                    break;
                default:
                    System.err.println("unknown command");
                    System.exit(1);
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
