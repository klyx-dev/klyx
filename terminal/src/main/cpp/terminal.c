#include <jni.h>
#include <dirent.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>
#include <errno.h>


int term_create_subprocess(const char *cmd, const char *cwd, char **argv, char **envp, int *pProcessId, int rows, int columns, int cell_width, int cell_height, char **error_message) {
    int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm < 0) {
        *error_message = "Cannot open /dev/ptmx";
        return -1;
    }

#ifdef LACKS_PTSNAME_R
    char* devname;
#else
    char devname[64];
#endif
    if (grantpt(ptm) || unlockpt(ptm) ||
#ifdef LACKS_PTSNAME_R
            (devname = ptsname(ptm)) == NULL
#else
            ptsname_r(ptm, devname, sizeof(devname))
#endif
            ) {
        *error_message = "Cannot grantpt()/unlockpt()/ptsname_r() on /dev/ptmx";
        return -1;
    }

    struct termios tios;
    tcgetattr(ptm, &tios);
    tios.c_iflag |= IUTF8;
    tios.c_iflag &= ~(IXON | IXOFF);
    tcsetattr(ptm, TCSANOW, &tios);

    struct winsize sz = {.ws_row = (unsigned short) rows, .ws_col = (unsigned short) columns, .ws_xpixel = (unsigned short) (columns * cell_width), .ws_ypixel = (unsigned short) (rows * cell_height)};
    ioctl(ptm, TIOCSWINSZ, &sz);

    pid_t pid = fork();
    if (pid < 0) {
        *error_message = "Fork failed";
        return -1;
    } else if (pid > 0) {
        *pProcessId = (int) pid;
        return ptm;
    } else {
        sigset_t signals_to_unblock;
        sigfillset(&signals_to_unblock);
        sigprocmask(SIG_UNBLOCK, &signals_to_unblock, 0);

        close(ptm);
        setsid();

        int pts = open(devname, O_RDWR);
        if (pts < 0) exit(-1);

        dup2(pts, 0);
        dup2(pts, 1);
        dup2(pts, 2);

        DIR *self_dir = opendir("/proc/self/fd");
        if (self_dir != NULL) {
            int self_dir_fd = dirfd(self_dir);
            struct dirent *entry;
            while ((entry = readdir(self_dir)) != NULL) {
                int fd = atoi(entry->d_name);
                if (fd > 2 && fd != self_dir_fd) close(fd);
            }
            closedir(self_dir);
        }

        clearenv();
        if (envp) for (; *envp; ++envp) putenv(*envp);

        if (chdir(cwd) != 0) {
            char *err_msg;
            if (asprintf(&err_msg, "chdir(\"%s\")", cwd) == -1) err_msg = "chdir()";
            perror(err_msg);
            fflush(stderr);
        }
        execvp(cmd, argv);

        char *err_msg;
        if (asprintf(&err_msg, "exec(\"%s\")", cmd) == -1) err_msg = "exec()";
        perror(err_msg);
        _exit(1);
    }
}

void term_set_pty_window_size(int fd, int rows, int cols, int cell_width, int cell_height) {
    struct winsize sz = {.ws_row = (unsigned short) rows, .ws_col = (unsigned short) cols, .ws_xpixel = (unsigned short) (cols * cell_width), .ws_ypixel = (unsigned short) (rows * cell_height)};
    ioctl(fd, TIOCSWINSZ, &sz);
}

void term_set_pty_utf8_mode(int fd) {
    struct termios tios;
    tcgetattr(fd, &tios);
    if ((tios.c_iflag & IUTF8) == 0) {
        tios.c_iflag |= IUTF8;
        tcsetattr(fd, TCSANOW, &tios);
    }
}

int term_wait_for(int pid) {
    int status;
    waitpid(pid, &status, 0);
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
        return -WTERMSIG(status);
    } else {
        return 0;
    }
}

void term_close(int fd) {
    close(fd);
}

ssize_t term_read_from_fd(int fd, void *buffer, size_t max_len) {
    return read(fd, buffer, max_len);
}

size_t term_write_to_fd(int fd, const void *buffer, size_t len) {
    size_t written = 0;
    const char *ptr = (const char *) buffer;

    while (written < len) {
        ssize_t n = write(fd, ptr + written, len - written);

        if (n < 0) {
            if (errno == EINTR) continue;
            return -1;
        }

        if (n == 0) break;

        written += n;
    }

    return written;
}

ssize_t term_read_symlink(const char *path, char *buffer, size_t max_len) {
    ssize_t n = readlink(path, buffer, max_len - 1);
    if (n >= 0) {
        // C requires us to manually null-terminate the string
        buffer[n] = '\0';
    }
    return n;
}

int term_kill_process(int pid, int signal) {
    return kill(pid, signal);
}

static int throw_runtime_exception(JNIEnv *env, char const *message) {
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, exClass, message);
    return -1;
}

JNIEXPORT jint JNICALL
Java_com_klyx_terminal_native_Native_createSubprocess(JNIEnv *env, jclass clazz, jstring cmd, jstring cwd, jobjectArray args, jobjectArray env_vars, jintArray process_id_array, jint rows, jint columns, jint cell_width, jint cell_height) {
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

    size = env_vars ? (*env)->GetArrayLength(env, env_vars) : 0;
    char **envp = NULL;
    if (size > 0) {
        envp = (char **) malloc((size + 1) * sizeof(char *));
        if (!envp) return throw_runtime_exception(env, "malloc() for envp array failed");
        for (int i = 0; i < size; ++i) {
            jstring env_java_string = (jstring) (*env)->GetObjectArrayElement(env, env_vars, i);
            char const *env_utf8 = (*env)->GetStringUTFChars(env, env_java_string, NULL);
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

    int *pProcId = (int *) (*env)->GetPrimitiveArrayCritical(env, process_id_array, NULL);
    if (!pProcId) return throw_runtime_exception(env, "JNI call GetPrimitiveArrayCritical failed");

    *pProcId = procId;
    (*env)->ReleasePrimitiveArrayCritical(env, process_id_array, pProcId, 0);

    return ptm;
}

JNIEXPORT void JNICALL
native_setPtyWindowSize(jint fd, jint rows, jint cols, jint cell_width, jint cell_height) {
    term_set_pty_window_size(fd, rows, cols, cell_width, cell_height);
}

JNIEXPORT void JNICALL
native_setPtyUTF8Mode(jint fd) {
    term_set_pty_utf8_mode(fd);
}

JNIEXPORT jint JNICALL
Java_com_klyx_terminal_native_Native_waitFor(JNIEnv *env, jclass clazz, jint pid) {
    return term_wait_for(pid);
}

JNIEXPORT void JNICALL
native_close(jint fd) {
    term_close(fd);
}

JNIEXPORT jint JNICALL
Java_com_klyx_terminal_native_Native_readFromFd(JNIEnv *env, jclass clazz, jint fd, jbyteArray buffer, jint max_len) {
    jbyte *buf_ptr = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (!buf_ptr) return -1;

    ssize_t n = term_read_from_fd(fd, buf_ptr, (size_t) max_len);

    (*env)->ReleaseByteArrayElements(env, buffer, buf_ptr, 0);
    return (jint) n;
}

JNIEXPORT jint JNICALL
Java_com_klyx_terminal_native_Native_writeToFd(JNIEnv *env, jclass clazz, jint fd, jbyteArray buffer, jint len) {
    jbyte *buf_ptr = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (!buf_ptr) return -1;

    size_t n = term_write_to_fd(fd, buf_ptr, (size_t) len);

    (*env)->ReleaseByteArrayElements(env, buffer, buf_ptr, JNI_ABORT);
    return (jint) n;
}

JNIEXPORT jstring JNICALL
Java_com_klyx_terminal_native_Native_readSymlink(JNIEnv *env, jclass clazz, jstring path) {
    const char *path_utf8 = (*env)->GetStringUTFChars(env, path, NULL);
    char buffer[PATH_MAX];

    ssize_t n = term_read_symlink(path_utf8, buffer, sizeof(buffer));
    (*env)->ReleaseStringUTFChars(env, path, path_utf8);

    if (n < 0) return NULL;
    return (*env)->NewStringUTF(env, buffer);
}

JNIEXPORT jint JNICALL
Java_com_klyx_terminal_native_Native_killProcess(JNIEnv *env, jclass clazz, jint pid, jint signal) {
    return term_kill_process(pid, signal);
}

static JNINativeMethod gCriticalMethods[] = {
        {"setPtyWindowSize", "(IIIII)V", (void *) native_setPtyWindowSize},
        {"setPtyUTF8Mode", "(I)V", (void *) native_setPtyUTF8Mode},
        {"close", "(I)V", (void *) native_close}
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = (*env)->FindClass(env, "com/klyx/terminal/native/Native");
    if (!clazz) return JNI_ERR;

    int rc = (*env)->RegisterNatives(env, clazz, gCriticalMethods, sizeof(gCriticalMethods) / sizeof(gCriticalMethods[0]));
    if (rc < 0) return JNI_ERR;

    return JNI_VERSION_1_6;
}
