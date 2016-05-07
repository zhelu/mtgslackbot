# Slack bot for MtG cards, set, and rules

## Building

### Build serialied resources

```shell
bazel build java/lu/zhe/mtgslackbot:Resources $absolute_path_to_java/lu/zhe/mtgslackbot/res$
```

### Build runner

```shell
bazel run java/lu/zhe/mtgslackbot:Runner
```
