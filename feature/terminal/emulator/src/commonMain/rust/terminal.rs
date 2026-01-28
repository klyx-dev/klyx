use libc::open;
use std::ffi::CString;
use std::fs;

uniffi::setup_scaffolding!();

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum TerminalError {
    #[error("Failed to open /dev/ptmx: {0}")]
    PtmxOpenError(String),
    #[error("Failed to grantpt/unlockpt/ptsname: {0}")]
    PtySetupError(String),
    #[error("Fork failed: {0}")]
    ForkError(String),
    #[error("Failed to allocate memory")]
    AllocationError,
    #[error("Failed to change directory: {0}")]
    ChdirError(String),
    #[error("Failed to execute command: {0}")]
    ExecError(String),
    #[error("IO error: {0}")]
    IoError(String),
}

#[derive(uniffi::Record)]
pub struct PtyProcess {
    pub ptm_fd: i32,
    pub pid: i32,
}

#[uniffi::export]
pub fn create_subprocess(
    cmd: String,
    cwd: String,
    args: Vec<String>,
    env_vars: Vec<String>,
    rows: i32,
    columns: i32,
    cell_width: i32,
    cell_height: i32,
) -> Result<PtyProcess, TerminalError> {
    unsafe {
        std::env::set_var("RUST_BACKTRACE", "full");

        let ptm = open(
            b"/dev/ptmx\0".as_ptr() as *const libc::c_char,
            libc::O_RDWR | libc::O_CLOEXEC,
        );

        if ptm < 0 {
            return Err(TerminalError::PtmxOpenError(
                std::io::Error::last_os_error().to_string(),
            ));
        }

        // Grant access to slave, unlock it, and get its name
        if libc::grantpt(ptm) != 0 || libc::unlockpt(ptm) != 0 {
            libc::close(ptm);
            return Err(TerminalError::PtySetupError(
                std::io::Error::last_os_error().to_string(),
            ));
        }

        #[cfg(target_os = "macos")]
        let devname = {
            let name_ptr = libc::ptsname(ptm);
            if name_ptr.is_null() {
                libc::close(ptm);
                return Err(TerminalError::PtySetupError("ptsname failed".to_string()));
            }
            std::ffi::CStr::from_ptr(name_ptr).to_owned()
        };

        #[cfg(not(target_os = "macos"))]
        let devname = {
            let mut buf = [0u8; 64];
            if libc::ptsname_r(ptm, buf.as_mut_ptr() as *mut libc::c_char, buf.len()) != 0 {
                libc::close(ptm);
                return Err(TerminalError::PtySetupError("ptsname_r failed".to_string()));
            }
            std::ffi::CStr::from_ptr(buf.as_ptr() as *const libc::c_char).to_owned()
        };

        let mut tios: libc::termios = std::mem::zeroed();
        libc::tcgetattr(ptm, &mut tios);
        tios.c_iflag |= libc::IUTF8;
        tios.c_iflag &= !(libc::IXON | libc::IXOFF);
        libc::tcsetattr(ptm, libc::TCSANOW, &tios);

        let winsize = libc::winsize {
            ws_row: rows as u16,
            ws_col: columns as u16,
            ws_xpixel: (columns * cell_width) as u16,
            ws_ypixel: (rows * cell_height) as u16,
        };
        libc::ioctl(ptm, libc::TIOCSWINSZ, &winsize);

        let pid = libc::fork();

        if pid < 0 {
            libc::close(ptm);
            return Err(TerminalError::ForkError(
                std::io::Error::last_os_error().to_string(),
            ));
        } else if pid > 0 {
            // parent process
            return Ok(PtyProcess { ptm_fd: ptm, pid });
        } else {
            // child process

            // unblock all signals
            let mut signals_to_unblock: libc::sigset_t = std::mem::zeroed();
            libc::sigfillset(&mut signals_to_unblock);
            libc::sigprocmask(libc::SIG_UNBLOCK, &signals_to_unblock, std::ptr::null_mut());

            libc::close(ptm);
            libc::setsid();

            // open slave side of pty
            let pts = libc::open(devname.as_ptr(), libc::O_RDWR);
            if pts < 0 {
                libc::_exit(1);
            }

            // redirect stdin, stdout, stderr to pts
            libc::dup2(pts, 0);
            libc::dup2(pts, 1);
            libc::dup2(pts, 2);

            // close all other file descriptors
            let max_fd = libc::sysconf(libc::_SC_OPEN_MAX);
            if max_fd > 0 {
                for fd in 3..max_fd {
                    libc::close(fd as i32);
                }
            }

            // clear environment and set new one
            libc::clearenv();
            for env_var in &env_vars {
                let env_cstr = CString::new(env_var.as_bytes()).unwrap();
                libc::putenv(env_cstr.into_raw());
            }

            // change directory
            let cwd_cstr = CString::new(cwd.as_bytes()).unwrap();
            if libc::chdir(cwd_cstr.as_ptr()) != 0 {
                let error = std::io::Error::last_os_error();
                eprintln!("chdir(\"{}\") failed: {}", cwd, error);
                use std::io::Write;
                let _ = std::io::stderr().flush();
            }

            // prepare arguments for execvp
            let cmd_cstr = CString::new(cmd.as_bytes()).unwrap();
            let argv_vec: Vec<CString> = args
                .iter()
                .map(|arg| CString::new(arg.as_bytes()).unwrap())
                .collect();

            let mut argv_ptrs: Vec<*const libc::c_char> =
                argv_vec.iter().map(|arg| arg.as_ptr()).collect();
            argv_ptrs.push(std::ptr::null());

            libc::execvp(
                cmd_cstr.as_ptr(),
                argv_ptrs.as_ptr() as *const *const libc::c_char,
            );

            // if we get here, exec failed
            let error = std::io::Error::last_os_error();
            eprintln!("exec(\"{}\") failed: {}", cmd, error);
            libc::_exit(1);
        }
    }
}

#[uniffi::export]
pub fn set_pty_window_size(
    fd: i32,
    rows: u32,
    cols: u32,
    cell_width: u32,
    cell_height: u32,
) {
    unsafe {
        let winsize = libc::winsize {
            ws_row: rows as u16,
            ws_col: cols as u16,
            ws_xpixel: (cols * cell_width) as u16,
            ws_ypixel: (rows * cell_height) as u16,
        };
        libc::ioctl(fd, libc::TIOCSWINSZ, &winsize);
    }
}

#[uniffi::export]
pub fn set_pty_utf8_mode(fd: i32) {
    unsafe {
        let mut tios: libc::termios = std::mem::zeroed();
        libc::tcgetattr(fd, &mut tios);
        if (tios.c_iflag & libc::IUTF8) == 0 {
            tios.c_iflag |= libc::IUTF8;
            libc::tcsetattr(fd, libc::TCSANOW, &tios);
        }
    }
}

#[uniffi::export]
pub fn wait_for(pid: i32) -> i32 {
    unsafe {
        let mut status: libc::c_int = 0;
        libc::waitpid(pid, &mut status, 0);

        if libc::WIFEXITED(status) {
            libc::WEXITSTATUS(status)
        } else if libc::WIFSIGNALED(status) {
            -libc::WTERMSIG(status)
        } else {
            0
        }
    }
}

#[uniffi::export]
pub fn close_fd(fd: i32) {
    eprintln!("RUST closing fd {}", fd);
    unsafe {
        libc::close(fd);
    }
}

#[uniffi::export]
pub fn read_from_fd(fd: i32, max_len: u32) -> Result<Vec<u8>, TerminalError> {
    let mut buf = vec![0u8; max_len as usize];

    let n = unsafe {
        libc::read(
            fd,
            buf.as_mut_ptr() as *mut libc::c_void,
            buf.len(),
        )
    };

    if n < 0 {
        Err(TerminalError::IoError(
            std::io::Error::last_os_error().to_string()
        ))
    } else {
        buf.truncate(n as usize);
        Ok(buf)
    }
}

#[uniffi::export]
pub fn write_to_fd(fd: i32, buffer: Vec<u8>) -> Result<i32, TerminalError> {
    let mut written = 0usize;

    while written < buffer.len() {
        let n = unsafe {
            libc::write(
                fd,
                buffer[written..].as_ptr() as *const libc::c_void,
                buffer.len() - written,
            )
        };

        if n < 0 {
            let err = std::io::Error::last_os_error();

            // retry if interrupted by signal
            if err.kind() == std::io::ErrorKind::Interrupted {
                continue;
            }

            return Err(TerminalError::IoError(err.to_string()));
        }

        if n == 0 {
            break;
        }

        written += n as usize;
    }

    Ok(written as i32)
}

#[uniffi::export]
pub fn read_symlink(path: String) -> Result<String, TerminalError> {
    match std::fs::read_link(&path) {
        Ok(target) => {
            Ok(target.to_string_lossy().to_string())
        }
        Err(e) => Err(TerminalError::IoError(e.to_string()))
    }
}

#[uniffi::export]
pub fn kill_process(pid: i32, signal: i32) -> Result<(), TerminalError> {
    unsafe {
        if libc::kill(pid, signal) != 0 {
            Err(TerminalError::IoError(
                std::io::Error::last_os_error().to_string()
            ))
        } else {
            Ok(())
        }
    }
}

