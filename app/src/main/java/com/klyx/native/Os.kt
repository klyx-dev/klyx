package com.klyx.native

import dalvik.annotation.optimization.FastNative

object Os {
    @JvmStatic
    @FastNative
    external fun getpwuid(uid: Int): Passwd?

    @JvmStatic
    @FastNative
    external fun getgrgid(gid: Int): Group?
}

data class Passwd(
    val pw_name: String,
    val pw_uid: Int,
    val pw_gid: Int,
    val pw_dir: String,
    val pw_shell: String
)

data class Group(val gr_name: String, val gr_gid: Int)
