licenses(["notice"])

package(default_visibility = ["//visibility:public"])

filegroup(
    name = "nomad",
    srcs = select({
        "//tools/platform:darwin": ["@nomad_mac//:nomad-scheduler"],
        "//conditions:default": ["@nomad_linux//:nomad-scheduler"],
    }),
)
