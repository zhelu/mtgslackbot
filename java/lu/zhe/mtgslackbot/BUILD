package(default_visibility = ["//java/lu/zhe/mtgslackbot:__subpackages__"])

load("@io_bazel_rules_appengine//appengine:appengine.bzl", "appengine_war")

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
    resources = glob(["res/*.ser"]),
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

java_binary(
    name = "servlet",
    srcs = ["MtgslackbotServlet.java"],
    deps = [
        ":data_sources",
        "//java/lu/zhe/mtgslackbot/parsing:parsing",
        "@io_bazel_rules_appengine//appengine:javax.servlet.api",
    ],
    main_class = "not.used",
)

java_binary(
    name = "runner",
    srcs = ["Runner.java"],
    deps = [
        ":data_sources",
        "//java/lu/zhe/mtgslackbot/parsing:parsing",
        "@com_google_guava_guava//jar",
        "@io_bazel_rules_appengine//appengine:javax.servlet.api",
    ],
    main_class = "lu.zhe.mtgslackbot.Runner",
)

appengine_war(
    name = "mtgslackbot",
    data = [":webapp"],
    data_path = "/webapp",
    jars = ["//java/lu/zhe/mtgslackbot:MtgslackbotServlet_deploy.jar"],
)

filegroup(
    name = "webapp",
    srcs = glob(["webapp/**/*"]),
)
