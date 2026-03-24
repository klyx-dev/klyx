#include "terminal.h"
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

#ifdef __APPLE__
# define LACKS_PTSNAME_R
#endif

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

ssize_t term_write_to_fd(int fd, const void *buffer, size_t len) {
    size_t written = 0;
    const char *ptr = (const char *) buffer;

    while (written < len) {
        ssize_t n = write(fd, ptr + written, len - written);

        if (n < 0) {
            // Retry if interrupted by signal, just like Rust's ErrorKind::Interrupted
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
