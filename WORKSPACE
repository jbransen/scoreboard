workspace(name = "scoreboard")

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

android_sdk_repository(
  name = "androidsdk",
  path = "/usr/local/android-sdk",
  api_level = 26,
  build_tools_version = "27.0.3",
)

skylib_version = "0.8.0"
http_archive(
  name = "bazel_skylib",
  type = "tar.gz",
  url = "https://github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib.{}.tar.gz".format (skylib_version, skylib_version),
  sha256 = "2ef429f5d7ce7111263289644d233707dba35e39696377ebab8b0bc701f7818e",
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")
bazel_skylib_workspace()

git_repository(
  name = 'com_google_protobuf',
  remote = 'https://github.com/google/protobuf',
  commit = "09745575a923640154bcf307fba8aedff47f240a",
  shallow_since = "1558721209 -0700",
)

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
protobuf_deps()

git_repository(
  name = "org_pubref_rules_maven",
  remote = "https://github.com/pubref/rules_maven",
  commit = "339c378f856461add63f155d82077de5813e649e",
  shallow_since = "1555185650 -0600",
)

maven_jar(
  name = "error_prone_annotations_maven",
  artifact = "com.google.errorprone:error_prone_annotations:2.3.2",
)

bind(
  name = "error_prone_annotations",
  actual = "@error_prone_annotations_maven//jar",
)

maven_jar(
  name = "jewelcli",
  artifact = "com.lexicalscope.jewelcli:jewelcli:0.8.9",
)

maven_jar(
  name = "okhttp",
  artifact = "com.squareup.okhttp3:okhttp:3.12.0",
)

maven_jar(
  name = "okhttp_mockwebserver",
  artifact = "com.squareup.okhttp3:mockwebserver:3.12.0",
)

maven_jar(
  name = "okio",
  artifact = "com.squareup.okio:okio:1.13.0",
)

maven_jar(
 name = "gson_maven",
 artifact = "com.google.code.gson:gson:2.8.2",
)

bind(
  name = "gson",
  actual = "@gson_maven//jar",
)

maven_jar(
  name = "guava_maven",
  artifact = "com.google.guava:guava:24.0-android",
)

bind(
  name = "guava",
  actual = "@guava_maven//jar",
)

maven_jar(
  name = "jfoenix",
  artifact = "com.jfoenix:jfoenix:1.0.0",
)

maven_jar(
  name = "junit4",
  artifact = "junit:junit:4.12",
)

maven_jar(
  name = "mockito",
  artifact = "org.mockito:mockito-all:1.10.19",
)

maven_jar(
  name = "truth",
  artifact = "com.google.truth:truth:0.27",
)

maven_jar(
  name = "nullable",
  artifact = "com.google.code.findbugs:jsr305:3.0.2",
)

maven_jar(
  name = "apachecommonslang",
  artifact = "org.apache.commons:commons-lang3:3.7",
)

maven_jar(
    name = "jtwig",
    artifact = "org.jtwig:jtwig-core:5.87.0.RELEASE",
)

maven_jar(
  name = "jtwig_reflection",
  artifact = "org.jtwig:jtwig-reflection:5.87.0.RELEASE",
)

maven_jar(
  name = "slf4j_api",
  artifact = "org.slf4j:slf4j-api:1.7.25",
)

maven_jar(
  name = "slf4j_simple",
  artifact = "org.slf4j:slf4j-simple:1.7.25",
)

load("@org_pubref_rules_maven//maven:rules.bzl", "maven_repositories")
maven_repositories()

load("@org_pubref_rules_maven//maven:rules.bzl", "maven_repository")
maven_repository(
  name = "parboiled_java",
  deps = ["org.parboiled:parboiled-java:1.1.8"],
  transitive_deps = [
    '4ce3ecdc7115bcbf9d4ff4e6ec638e60760819df:org.ow2.asm:asm:5.2',
    '2de10833bb3ade1939b1489b7656c9de20343e14:org.ow2.asm:asm-analysis:5.2',
    '733a8d67f6f4174d12142b7bbcfc496a6d99882e:org.ow2.asm:asm-tree:5.2',
    '9408ea14e73b7c9b427545a1b84923d6afaf8e1e:org.ow2.asm:asm-util:5.2',
    'af12604d2e555c65107d744bc502dcd9d1c0cad9:org.parboiled:parboiled-core:1.1.8',
    'df6ca179d7a1cd0b556a3f46c219503d0ab80388:org.parboiled:parboiled-java:1.1.8',
  ],
)

load("@parboiled_java//:rules.bzl", "parboiled_java_compile")
parboiled_java_compile()

maven_jar(
  name = "concurrentlinkedhashmap",
  artifact = "com.googlecode.concurrentlinkedhashmap:concurrentlinkedhashmap-lru:1.4.2",
)

maven_jar(
  name = "auto_value",
  artifact = "com.google.auto.value:auto-value:1.6.3",
  sha1 = "8edb6675b9c09ffdcc19937428e7ef1e3d066e12",
)

maven_jar(
  name = "auto_value_annotations",
  artifact = "com.google.auto.value:auto-value-annotations:1.6.3",
  sha1 = "b88c1bb7f149f6d2cc03898359283e57b08f39cc",
)

LWJGL_VER = "3.2.2"

maven_jar(
  name = "lwjgl",
  artifact = "org.lwjgl:lwjgl:jar:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_natives_linux",
  artifact = "org.lwjgl:lwjgl:jar:natives-linux:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_natives_windows",
  artifact = "org.lwjgl:lwjgl:jar:natives-windows:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_natives_macos",
  artifact = "org.lwjgl:lwjgl:jar:natives-macos:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_glfw",
  artifact = "org.lwjgl:lwjgl-glfw:jar:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_glfw_natives_linux",
  artifact = "org.lwjgl:lwjgl-glfw:jar:natives-linux:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_glfw_natives_windows",
  artifact = "org.lwjgl:lwjgl-glfw:jar:natives-windows:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_glfw_natives_macos",
  artifact = "org.lwjgl:lwjgl-glfw:jar:natives-macos:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_opengl",
  artifact = "org.lwjgl:lwjgl-opengl:jar:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_opengl_natives_linux",
  artifact = "org.lwjgl:lwjgl-opengl:jar:natives-linux:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_opengl_natives_windows",
  artifact = "org.lwjgl:lwjgl-opengl:jar:natives-windows:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_opengl_natives_macos",
  artifact = "org.lwjgl:lwjgl-opengl:jar:natives-macos:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_stb",
  artifact = "org.lwjgl:lwjgl-stb:jar:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_stb_natives_linux",
  artifact = "org.lwjgl:lwjgl-stb:jar:natives-linux:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_stb_natives_windows",
  artifact = "org.lwjgl:lwjgl-stb:jar:natives-windows:{}".format(LWJGL_VER),
)

maven_jar(
  name = "lwjgl_stb_natives_macos",
  artifact = "org.lwjgl:lwjgl-stb:jar:natives-macos:{}".format(LWJGL_VER),
)

http_archive(
  name = "six_archive",
  url = "https://pypi.python.org/packages/source/s/six/six-1.10.0.tar.gz#md5=34eed507548117b2ab523ab14b2f8b55",
  sha256 = "105f8d68616f8248e24bf0e9372ef04d3cc10104f1980f54d57b2ce73a5ad56a",
  strip_prefix = "six-1.10.0",
  build_file_content = """
py_library(
    name = "six",
    srcs = ["six.py"],
    visibility = ["//visibility:public"],
    srcs_version = "PY2AND3",
)
""",
)

bind(
  name = "six",
  actual = "@six_archive//:six",
)
