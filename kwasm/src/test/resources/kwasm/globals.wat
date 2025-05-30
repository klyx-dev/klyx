(module
    (global (export "i32") (mut i32) (i32.const 1))
    (global (export "i32_immut") i32 (i32.const 1))
    (global (export "i64") (mut i64) (i64.const 2))
    (global (export "i64_immut") i64 (i64.const 2))
    (global (export "f32") (mut f32) (f32.const 3.0))
    (global (export "f32_immut") f32 (f32.const 3.0))
    (global (export "f64") (mut f64) (f64.const 4.0))
    (global (export "f64_immut") f64 (f64.const 4.0)))
