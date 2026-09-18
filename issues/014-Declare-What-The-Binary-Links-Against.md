# 014 — Declare the libraries the native binary links against

**Priority:** 1
**Opened:** 2026-09-18
**Source:** found while answering Agent Sluice's packaging question (QL6) in the channel

## What

`sokar-agent-pi` is a native binary linked dynamically against `libc.so.6` and `libz.so.1`. Neither package
says so: the `.deb` depends only on `sokar`, the `.rpm` requires only `sokar`. On a host whose
glibc is too old, or without zlib, the package installs cleanly and the binary then fails to start -
far from the install that caused it.

**Measured on the binary** with `readelf -d -V`: it needs `libc.so.6` up to symbol version
`GLIBC_2.34` and `libz.so.1` up to `ZLIB_1.2.2`. The floor comes from the symbols used, not from the
glibc of the machine that built it: built on glibc 2.43, it still asks for no more than 2.34.

Nothing resolves this by itself. dpkg and apt do not read an ELF file; `dpkg-shlibdeps` does, at
build time, and jdeb does not run it. The rpm plugin does not generate the requirements `rpmbuild`
would.

## What would close it

- The deb declares `libc6 (>= 2.34)` and `zlib1g (>= 1:1.2.2)`; the rpm requires
  `libc.so.6(GLIBC_2.34)(64bit)` and `libz.so.1(ZLIB_1.2.2)(64bit)`, the names `rpmbuild` itself
  generates. Both floors come from one place in `pom.xml`.
- The build fails when the binary needs a library the packages do not declare, or a newer symbol
  version than they promise - checked on the binary just built, before it is packed.
- Measured: both packages install on Ubuntu and Fedora in the acceptance run with the new
  dependencies.
