#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include "terminal.h"

#define KLYX_UNUSED(x) x __attribute__((__unused__))

static int throw_runtime_exception(JNIEnv *env, char const *message) {
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, exClass, message);
    return -1;
}

JNIEXPORT jint JNICALL Java_com_klyx_terminal_c_TerminalNative_createSubprocess(
        JNIEnv *env, jclass KLYX_UNUSED(clazz), jstring cmd, jstring cwd,
        jobjectArray args, jobjectArray envVars, jintArray processIdArray,
        jint rows, jint columns, jint cell_width, jint cell_height) {

    jsize size = args ? (*env)->GetArrayLength(env, args) : 0;
    char **argv = NULL;
    if (size > 0) {
        argv = (char **) malloc((size + 1) * sizeof(char *));
        if (!argv) return throw_runtime_exception(env, "Couldn't allocate argv array");
        for (int i = 0; i < size; ++i) {
            jstring arg_java_string = (jstring) (*env)->GetObjectArrayElement(env, args, i);
            char const *arg_utf8 = (*env)->GetStringUTFChars(env, arg_java_string, NULL);
            if (!arg_utf8) return throw_runtime_exception(env, "GetStringUTFChars() failed for argv");
            argv[i] = strdup(arg_utf8);
            (*env)->ReleaseStringUTFChars(env, arg_java_string, arg_utf8);
        }
        argv[size] = NULL;
    }

    size = envVars ? (*env)->GetArrayLength(env, envVars) : 0;
    char **envp = NULL;
    if (size > 0) {
        envp = (char **) malloc((size + 1) * sizeof(char *));
        if (!envp) return throw_runtime_exception(env, "malloc() for envp array failed");
        for (int i = 0; i < size; ++i) {
            jstring env_java_string = (jstring) (*env)->GetObjectArrayElement(env, envVars, i);
            char const *env_utf8 = (*env)->GetStringUTFChars(env, env_java_string, 0);
            if (!env_utf8) return throw_runtime_exception(env, "GetStringUTFChars() failed for env");
            envp[i] = strdup(env_utf8);
            (*env)->ReleaseStringUTFChars(env, env_java_string, env_utf8);
        }
        envp[size] = NULL;
    }

    int procId = 0;
    char *error_message = NULL;
    char const *cmd_cwd = (*env)->GetStringUTFChars(env, cwd, NULL);
    char const *cmd_utf8 = (*env)->GetStringUTFChars(env, cmd, NULL);

    int ptm = term_create_subprocess(cmd_utf8, cmd_cwd, argv, envp, &procId, rows, columns, cell_width, cell_height, &error_message);

    (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);
    (*env)->ReleaseStringUTFChars(env, cwd, cmd_cwd);

    if (argv) {
        for (char **tmp = argv; *tmp; ++tmp) free(*tmp);
        free(argv);
    }
    if (envp) {
        for (char **tmp = envp; *tmp; ++tmp) free(*tmp);
        free(envp);
    }

    if (ptm < 0 && error_message != NULL) {
        return throw_runtime_exception(env, error_message);
    }

    int *pProcId = (int *) (*env)->GetPrimitiveArrayCritical(env, processIdArray, NULL);
    if (!pProcId) return throw_runtime_exception(env, "JNI call GetPrimitiveArrayCritical failed");

    *pProcId = procId;
    (*env)->ReleasePrimitiveArrayCritical(env, processIdArray, pProcId, 0);

    return ptm;
}

JNIEXPORT void JNICALL Java_com_klyx_terminal_c_TerminalNative_setPtyWindowSize(
        JNIEnv *KLYX_UNUSED(env), jclass KLYX_UNUSED(clazz), jint fd, jint rows, jint cols, jint cell_width, jint cell_height) {
    term_set_pty_window_size(fd, rows, cols, cell_width, cell_height);
}

JNIEXPORT void JNICALL Java_com_klyx_terminal_c_TerminalNative_setPtyUTF8Mode(
        JNIEnv *KLYX_UNUSED(env), jclass KLYX_UNUSED(clazz), jint fd) {
    term_set_pty_utf8_mode(fd);
}

JNIEXPORT jint JNICALL Java_com_klyx_terminal_c_TerminalNative_waitFor(
        JNIEnv *KLYX_UNUSED(env), jclass KLYX_UNUSED(clazz), jint pid) {
    return term_wait_for(pid);
}

JNIEXPORT void JNICALL Java_com_klyx_terminal_c_TerminalNative_close(
        JNIEnv *KLYX_UNUSED(env), jclass KLYX_UNUSED(clazz), jint fileDescriptor) {
    term_close(fileDescriptor);
}

JNIEXPORT jint JNICALL Java_com_klyx_terminal_c_TerminalNative_readFromFd(
        JNIEnv *env, jclass KLYX_UNUSED(clazz), jint fd, jbyteArray buffer, jint max_len) {

    jbyte *buf_ptr = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (!buf_ptr) return -1;

    ssize_t n = term_read_from_fd(fd, buf_ptr, (size_t) max_len);

    // release and copy data back to Kotlin
    (*env)->ReleaseByteArrayElements(env, buffer, buf_ptr, 0);
    return (jint) n;
}

JNIEXPORT jint JNICALL Java_com_klyx_terminal_c_TerminalNative_writeToFd(
        JNIEnv *env, jclass KLYX_UNUSED(clazz), jint fd, jbyteArray buffer, jint len) {

    jbyte *buf_ptr = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (!buf_ptr) return -1;

    ssize_t n = term_write_to_fd(fd, buf_ptr, (size_t) len);

    // JNI_ABORT tells JVM not to copy data back, saving performance since it was read-only
    (*env)->ReleaseByteArrayElements(env, buffer, buf_ptr, JNI_ABORT);
    return (jint) n;
}

JNIEXPORT jstring JNICALL Java_com_klyx_terminal_c_TerminalNative_readSymlink(
        JNIEnv *env, jclass KLYX_UNUSED(clazz), jstring path) {

    const char *path_utf8 = (*env)->GetStringUTFChars(env, path, NULL);
    char buffer[PATH_MAX];

    ssize_t n = term_read_symlink(path_utf8, buffer, sizeof(buffer));
    (*env)->ReleaseStringUTFChars(env, path, path_utf8);

    if (n < 0) return NULL; // return null to Kotlin if symlink read fails

    return (*env)->NewStringUTF(env, buffer);
}

JNIEXPORT jint JNICALL Java_com_klyx_terminal_c_TerminalNative_killProcess(
        JNIEnv *env, jclass KLYX_UNUSED(clazz), jint pid, jint sig) {
    return term_kill_process(pid, sig);
}
