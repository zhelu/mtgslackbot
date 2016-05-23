# Slack bot for MtG cards, set, and rules

## Building

### Build serialied resources

```shell
bazel build java/lu/zhe/mtgslackbot:BuildResources $absolute_path_to_java/lu/zhe/mtgslackbot/res$
```

### Configuration
heroku config:set token=$token_value_from_slack
