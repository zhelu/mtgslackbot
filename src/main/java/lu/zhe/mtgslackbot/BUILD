package(default_visibility = ["//src/main/java/lu/zhe/mtgslackbot:__subpackages__"])

java_library(
    name = "resources",
    srcs = ["Resources.java"],
    deps = [
        "//src/main/java/lu/zhe/mtgslackbot/rule:rule",
        "@com_google_guava_guava//jar",
    ],
)

java_library(
    name = "mtgslackbot",
    srcs = ["MtgSlackbot.java"],
    deps = [
        "//src/main/java/lu/zhe/mtgslackbot:data_sources",
        "//src/main/java/lu/zhe/mtgslackbot/parsing",
        "@commons_logging//jar",
        "@com_sparkjava_spark_2_2//jar",
        "@org_apache_httpcomponents_httpclient//jar",
        "@org_apache_httpcomponents_httpcore//jar",
        "@org_json_json//jar",
    ],
)

java_library(
    name = "data_sources",
    srcs = ["DataSources.java"],
    deps = [
        "//src/main/java/lu/zhe/mtgslackbot/parsing:parsing",
        "//src/main/java/lu/zhe/mtgslackbot/shared:shared",
        "//src/main/resources/lu/zhe/mtgslackbot:res",
        "@com_google_guava_guava//jar",
        "@org_json_json//jar",
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
    name = "tester",
    srcs = ["Tester.java"],
    deps = [
        ":data_sources",
        "//src/main/java/lu/zhe/mtgslackbot/parsing:parsing",
        "@com_google_guava_guava//jar",
    ],
)

java_binary(
    name = "Tester",
    runtime_deps = [":tester",],
    main_class = "lu.zhe.mtgslackbot.Tester",
)

java_binary(
    name = "MtgSlackbot",
    runtime_deps = [
        ":mtgslackbot",
        "@javax_servlet_api_3_1_0//jar",
        "@org_slf4j_slf4j_api_1_6_6//jar",
        "@org_eclipse_jetty_http_9_3_10//jar",
        "@org_eclipse_jetty_io_9_3_10//jar",
        "@org_eclipse_jetty_server_9_3_10//jar",
        "@org_eclipse_jetty_util_9_3_10//jar",
    ],
    main_class = "lu.zhe.mtgslackbot.MtgSlackbot",
)
