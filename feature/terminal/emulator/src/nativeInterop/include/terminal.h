#ifndef KLYX_TERMINAL_H
#define KLYX_TERMINAL_H

#include <sys/types.h>

int term_create_subprocess(
        const char *cmd,
        const char *cwd,
        char **argv,
        char **envp,
        int *pProcessId,
        int rows,
        int columns,
        int cell_width,
        int cell_height,
        char **error_message
);

void term_set_pty_window_size(int fd, int rows, int cols, int cell_width, int cell_height);

void term_set_pty_utf8_mode(int fd);

int term_wait_for(int pid);

void term_close(int fd);

ssize_t term_read_from_fd(int fd, void *buffer, size_t max_len);

ssize_t term_write_to_fd(int fd, const void *buffer, size_t len);

ssize_t term_read_symlink(const char *path, char *buffer, size_t max_len);

int term_kill_process(int pid, int signal);

#endif // KLYX_TERMINAL_H
