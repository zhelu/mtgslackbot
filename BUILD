# The following dependencies were calculated from:
# com.google.auto.value:auto-value:1.2
# com.google.code.findbugs:jsr305:2.0.3
# com.google.guava:guava:16.0.1
# com.google.code.gson:gson:2.3
# junit:junit:4.11

java_library(
    name = "com_google_code_findbugs_jsr305",
    visibility = ["//visibility:public"],
    exports = [
        "@com_google_code_findbugs_jsr305//jar",
    ],
)

java_library(
    name = "org_hamcrest_hamcrest_core",
    visibility = ["//visibility:public"],
    exports = [
        "@org_hamcrest_hamcrest_core//jar",
    ],
)

java_library(
    name = "com_google_guava_guava",
    visibility = ["//visibility:public"],
    exports = [
        "@com_google_guava_guava//jar",
    ],
)

java_library(
    name = "com_google_code_gson_gson",
    visibility = ["//visibility:public"],
    exports = [
        "@com_google_code_gson_gson//jar",
    ],
)

java_library(
    name = "junit_junit",
    visibility = ["//visibility:public"],
    exports = [
        "@junit_junit//jar",
        "@org_hamcrest_hamcrest_core//jar",
    ],
)

java_plugin(
    name = "auto_plugin",
    visibility = ["//visibility:public"],
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = ["//external:auto_value-jar"],
)

java_library(
    name = "auto",
    visibility = ["//visibility:public"],
    exports = ["//external:auto_value-jar"],
    exported_plugins = [":auto_plugin"],
)
