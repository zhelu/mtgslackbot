# Slack bot for MtG cards, set, and rules

## Building

### Build serialied resources

```shell
bazel build java/lu/zhe/mtgslackbot:BuildResources $absolute_path_to_java/lu/zhe/mtgslackbot/res$
```

### Testing

```shell
bazel run java/lu/zhe/mtgslackbot:servlet -- \!mtg card serra angel
```

### Deploying to heroku
Run `push_heroku.sh`
