package(default_visibility = ["//src/main/java/lu/zhe/mtgslackbot:__subpackages__"])

java_library(
    name = "card",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/lu/zhe/mtgslackbot/shared:shared",
        "//:auto",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_code_gson_gson//jar",
        "@com_google_guava_guava//jar",
    ],
)
