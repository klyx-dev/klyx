(module
    (import "System" "println" (func $println (param i32 i32)))
    (memory (data "Hello world"))
    (func $main
        (call $println (i32.const 0) (i32.const 11)))
    (export "mem" (memory 0))
    (start $main))
