package(default_visibility = ["//java/lu/zhe/mtgslackbot:__subpackages__"])

java_library(
    name = "resources",
    srcs = ["Resources.java"],
    deps = [
        "@com_google_guava_guava//jar",
        "//java/lu/zhe/mtgslackbot/card:card",
        "//java/lu/zhe/mtgslackbot/rule:rule",
        "//java/lu/zhe/mtgslackbot/set:set",
    ],
)

java_library(
    name = "data_sources",
    srcs = ["DataSources.java"],
    deps = [
        "@com_google_guava_guava//jar",
        "//java/lu/zhe/mtgslackbot/card:card",
        "//java/lu/zhe/mtgslackbot/parsing:parsing",
    ],
)

java_binary(
    name = "BuildResources",
    runtime_deps = [
        ":resources",
    ],
    main_class = "lu.zhe.mtgslackbot.Resources",
)

java_library(
    name = "runner",
    srcs = ["Runner.java"],
    resources = glob(["res/*.ser"]),
    deps = [
        ":data_sources",
        "//java/lu/zhe/mtgslackbot/parsing:parsing",
    ],
)

java_binary(
    name = "Runner",
    main_class = "lu.zhe.mtgslackbot.Runner",
    runtime_deps = [
        ":runner",
    ],
)
