package(default_visibility = ["//src/main/java/lu/zhe/mtgslackbot:__subpackages__"])

java_library(
    name = "resources",
    srcs = ["Resources.java"],
    deps = [
        "//src/main/java/lu/zhe/mtgslackbot/card:card",
        "//src/main/java/lu/zhe/mtgslackbot/rule:rule",
        "//src/main/java/lu/zhe/mtgslackbot/set:set",
        "@com_google_guava_guava//jar",
    ],
)

java_library(
    name = "data_sources",
    srcs = ["DataSources.java"],
    resources = glob(["res/*.ser"]),
    deps = [
        "//src/main/java/lu/zhe/mtgslackbot/card:card",
        "//src/main/java/lu/zhe/mtgslackbot/parsing:parsing",
        "@com_google_guava_guava//jar",
    ],
)

java_binary(
    name = "BuildResources",
    runtime_deps = [
        ":resources",
    ],
    main_class = "lu.zhe.mtgslackbot.Resources",
)
