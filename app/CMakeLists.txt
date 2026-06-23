cmake_minimum_required(VERSION 3.22.1)

project(klyx C ASM)

set(CPP_DIR src/main/cpp)
set(PROOT_DIR ${CPP_DIR}/proot/src)
set(TALLOC_DIR ${CPP_DIR}/talloc-minimal)

add_library(klyx SHARED
        ${CPP_DIR}/os.c
)

target_link_libraries(klyx
        android
        log
)

add_library(talloc STATIC
        ${TALLOC_DIR}/talloc.c
)

target_include_directories(talloc PUBLIC
        ${TALLOC_DIR}
)

target_compile_definitions(talloc PRIVATE
        ANDROID
)

target_compile_options(talloc PRIVATE
        -Wno-error
)

add_executable(loader
        ${PROOT_DIR}/loader/loader.c
        ${PROOT_DIR}/loader/assembly.S
)

target_include_directories(loader PRIVATE
        ${PROOT_DIR}
)

target_compile_options(loader PRIVATE
        -fPIC
        -ffreestanding
)

target_link_options(loader PRIVATE
        -static
        -nostdlib
        -Wl,-z,noexecstack
)

set(PROOT_SOURCES
        ${PROOT_DIR}/cli/cli.c
        ${PROOT_DIR}/cli/proot.c
        ${PROOT_DIR}/cli/note.c
        ${PROOT_DIR}/execve/enter.c
        ${PROOT_DIR}/execve/exit.c
        ${PROOT_DIR}/execve/shebang.c
        ${PROOT_DIR}/execve/elf.c
        ${PROOT_DIR}/execve/ldso.c
        ${PROOT_DIR}/execve/auxv.c
        ${PROOT_DIR}/execve/aoxp.c
        ${PROOT_DIR}/path/binding.c
        ${PROOT_DIR}/path/glue.c
        ${PROOT_DIR}/path/canon.c
        ${PROOT_DIR}/path/f2fs-bug.c
        ${PROOT_DIR}/path/path.c
        ${PROOT_DIR}/path/proc.c
        ${PROOT_DIR}/path/temp.c
        ${PROOT_DIR}/syscall/seccomp.c
        ${PROOT_DIR}/syscall/syscall.c
        ${PROOT_DIR}/syscall/chain.c
        ${PROOT_DIR}/syscall/enter.c
        ${PROOT_DIR}/syscall/exit.c
        ${PROOT_DIR}/syscall/sysnum.c
        ${PROOT_DIR}/syscall/socket.c
        ${PROOT_DIR}/syscall/heap.c
        ${PROOT_DIR}/syscall/rlimit.c
        ${PROOT_DIR}/tracee/tracee.c
        ${PROOT_DIR}/tracee/mem.c
        ${PROOT_DIR}/tracee/reg.c
        ${PROOT_DIR}/tracee/event.c
        ${PROOT_DIR}/tracee/seccomp.c
        ${PROOT_DIR}/tracee/statx.c
        ${PROOT_DIR}/ptrace/ptrace.c
        ${PROOT_DIR}/ptrace/user.c
        ${PROOT_DIR}/ptrace/wait.c
        ${PROOT_DIR}/extension/extension.c
        ${PROOT_DIR}/extension/ashmem_memfd/ashmem_memfd.c
        ${PROOT_DIR}/extension/kompat/kompat.c
        ${PROOT_DIR}/extension/fake_id0/chown.c
        ${PROOT_DIR}/extension/fake_id0/chroot.c
        ${PROOT_DIR}/extension/fake_id0/getsockopt.c
        ${PROOT_DIR}/extension/fake_id0/sendmsg.c
        ${PROOT_DIR}/extension/fake_id0/socket.c
        ${PROOT_DIR}/extension/fake_id0/open.c
        ${PROOT_DIR}/extension/fake_id0/unlink.c
        ${PROOT_DIR}/extension/fake_id0/rename.c
        ${PROOT_DIR}/extension/fake_id0/chmod.c
        ${PROOT_DIR}/extension/fake_id0/utimensat.c
        ${PROOT_DIR}/extension/fake_id0/access.c
        ${PROOT_DIR}/extension/fake_id0/exec.c
        ${PROOT_DIR}/extension/fake_id0/link.c
        ${PROOT_DIR}/extension/fake_id0/symlink.c
        ${PROOT_DIR}/extension/fake_id0/mk.c
        ${PROOT_DIR}/extension/fake_id0/stat.c
        ${PROOT_DIR}/extension/fake_id0/helper_functions.c
        ${PROOT_DIR}/extension/fake_id0/fake_id0.c
        ${PROOT_DIR}/extension/hidden_files/hidden_files.c
        ${PROOT_DIR}/extension/mountinfo/mountinfo.c
        ${PROOT_DIR}/extension/port_switch/port_switch.c
        ${PROOT_DIR}/extension/sysvipc/sysvipc.c
        ${PROOT_DIR}/extension/sysvipc/sysvipc_msg.c
        ${PROOT_DIR}/extension/sysvipc/sysvipc_sem.c
        ${PROOT_DIR}/extension/sysvipc/sysvipc_shm.c
        ${PROOT_DIR}/extension/link2symlink/link2symlink.c
        ${PROOT_DIR}/extension/fix_symlink_size/fix_symlink_size.c
)

add_executable(proot
        ${PROOT_SOURCES}
)

target_compile_definitions(proot PRIVATE
        HAVE_PROCESS_VM
        HAVE_SECCOMP_FILTER
        PROOT_UNBUNDLE_LOADER
)

target_include_directories(proot PRIVATE
        ${PROOT_DIR}
        ${TALLOC_DIR}
)

find_program(READELF readelf REQUIRED)
find_program(AWK awk REQUIRED)

get_filename_component(
        LOADER_INFO_AWK
        "${PROOT_DIR}/loader/loader-info.awk"
        ABSOLUTE
)

add_custom_command(
        OUTPUT ${CMAKE_BINARY_DIR}/loader-info.c
        COMMAND ${READELF} -s $<TARGET_FILE:loader> | ${AWK} -f ${LOADER_INFO_AWK} > ${CMAKE_BINARY_DIR}/loader-info.c
        DEPENDS loader
        VERBATIM
)

target_sources(proot PRIVATE
        ${CMAKE_BINARY_DIR}/loader-info.c
)

target_compile_definitions(proot PRIVATE
        _FILE_OFFSET_BITS=64
        _GNU_SOURCE
)

target_compile_options(proot PRIVATE
        -Wall
        -Wextra
)

target_link_libraries(proot
        talloc
)

target_link_options(proot PRIVATE
        -Wl,-z,noexecstack
)

set_target_properties(proot PROPERTIES
        OUTPUT_NAME "proot"
        PREFIX "lib"
        SUFFIX ".so"
)

set_target_properties(loader PROPERTIES
        OUTPUT_NAME "loader"
        PREFIX "lib"
        SUFFIX ".so"
)
