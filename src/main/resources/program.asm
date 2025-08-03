.data
hello: .asciiz "Hello, Asm4J!\n"

.text
    li  $v0, 4
    la  $a0, hello
    syscall

    li  $v0, 10
    syscall
