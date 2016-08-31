# Slack bot for MtG cards, set, and rules

## Building

### Build serialied resources

```shell
bazel run src/main/java/lu/zhe/mtgslackbot:BuildResources -- $absolute_path_to_java/lu/zhe/mtgslackbot/res$
```

## Deploying

### Configuration
You need to authenticate against the token that Slack provides.
```shell
heroku config:set token=$token_value_from_slack
```

By default, the app goes to sleep after 30 minutes of inactivity. If you want to send a keep alive.
Provide the app name via the `appname` env variable. Also, you'll need to set the `keepalive` env variable
to something like 25.
```shell
heroku config:set appname=my_slack_app
heroku config:set keepalive=25
```

### Pushing to heroku
```shell
git push heroku master
```

## Testing

### Manual testing
```shell
bazel run src/main/java/lu/zhe/mtgslackbot:Tester -- card emrakul the
```
