#include <jni.h>
#include <dlfcn.h>
#include <tree_sitter/api.h>
#include <string>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_klyx_editor_treesitter_KlyxTreeSitter_loadLanguageHandle(
        JNIEnv *env,
        jclass,
        jstring lib_path,
        jstring symbol_name
) {
    const char *path = env->GetStringUTFChars(lib_path, nullptr);
    const char *sym  = env->GetStringUTFChars(symbol_name, nullptr);

    void *handle = dlopen(path, RTLD_NOW);
    if (!handle) {
        const char *err = dlerror();
        if (err) {
            env->ThrowNew(env->FindClass("java/lang/UnsatisfiedLinkError"), err);
        }
        env->ReleaseStringUTFChars(lib_path, path);
        env->ReleaseStringUTFChars(symbol_name, sym);
        return 0;
    }

    using ts_func = const TSLanguage *(*)();
    auto load = reinterpret_cast<ts_func>(dlsym(handle, sym));

    const TSLanguage *language = nullptr;
    if (!load) {
        const char *err = dlerror();
        if (err) {
            env->ThrowNew(env->FindClass("java/lang/UnsatisfiedLinkError"), err);
        }
    } else {
        language = load();
    }

    env->ReleaseStringUTFChars(lib_path, path);
    env->ReleaseStringUTFChars(symbol_name, sym);

    return reinterpret_cast<jlong>(language);
}
