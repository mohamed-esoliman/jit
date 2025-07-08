## Jit

Minimal Git-like version control system written in Java 17. Single local repository, no networking. Objects are content-addressed using SHA-1 with canonical serialization.

## Build and Run

- Requirements: Java 17, Maven
- Build: `mvn -q -DskipTests package`
- Run: `java -jar target/jit.jar <command> [args]`

## Commands

```
jit init
jit add <path...>
jit status
jit commit -m "<message>"
jit log
jit checkout [--force] <commitHash|branchName>
jit branch <name>
jit switch <branchName>
jit cat-file <hash>
jit ls-tree <hash>
```

## Quick Start

Shell alias for convenience:

```
alias jit='java -jar target/jit.jar'
```

Then run:

```
jit init
jit status
echo "hello" > a.txt
jit add a.txt
jit status
jit commit -m "add a"
jit log
```

## Object Model

- Blob: raw file bytes. Serialized as `blob <size>\n<bytes>`
- Tree: directory snapshot
  - Entry bytes: `<mode> <name>\0<20-byte binary SHA1>`
  - Serialized as `tree <size>\n<entries>`
- Commit: text block
  - `commit <size>\n`
  - `tree <hash>\n`
  - optional `parent <hash>\n`
  - `author <name> <email>\n`
  - `timestamp <iso8601>\n`
  - blank line
  - `<message>\n`

Objects are stored at `.jit/objects/<first2>/<remaining38>`. Hash is SHA‑1 of the exact serialized bytes.

## Index (Staging Area)

`.jit/index` stores tab-separated records:

```
path<TAB>blob<TAB>mode<TAB>mtime<TAB>size
```

`jit add` walks provided paths, ignores `.jit/` and respects `.jitignore` (supports `*` wildcard, repo-root relative). It writes blobs if missing and updates the index.

## Status

- Changes to be committed: index vs HEAD tree
- Changes not staged: working tree vs index (mtime/size, hash confirm)
- Untracked files: present in working tree, not in index, outside `.jit/`

## Commit

- Builds trees from index entries recursively
- Parent is the current `HEAD` commit if any
- Author from environment: `JIT_AUTHOR_NAME`, `JIT_AUTHOR_EMAIL` (defaults: `User`, `user@example.com`)
- Timestamp is current time in ISO‑8601 UTC
- Updates current branch ref or writes detached `HEAD`

## HEAD and Refs

- Symbolic: `HEAD` contains `ref: refs/heads/<branch>`
- Detached: `HEAD` contains a commit hash

`jit branch <name>` creates `refs/heads/<name>` pointing to the current commit (or empty if none). `jit switch <name>` sets `HEAD` symbolic to that ref. `jit checkout` updates the working directory to exactly match the target commit tree and updates the index; it refuses to overwrite local changes unless `--force`.

## Plumbing

- `jit cat-file <hash>` prints `<type> <size>` then payload
- `jit ls-tree <hash>` outputs `<mode> <type> <hash>\t<name>` for each entry

## Limitations

- No merges, rebase, or networking
- Single parent chain in log
- Simple ignore and change detection (mtime/size with hash confirm)

## Contributing

Contributions are welcome. Open an issue or PR with a clear description and steps to reproduce or validate.

## License

MIT. See `LICENSE`.
