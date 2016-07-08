# Slack bot for MtG cards, set, and rules

## Building

### Build serialied resources

```shell
bazel run src/main/java/lu/zhe/mtgslackbot:BuildResources -- $absolute_path_to_java/lu/zhe/mtgslackbot/res$
```

## Deploying

### Configuration
```shell
heroku config:set token=$token_value_from_slack
```

### Pushing to heroku
```shell
git push heroku master
```

## Testing

### Manual testing
```shell
blaze run src/main/java/lu/zhe/mtgslackbot:Tester -- card emrakul the
```
