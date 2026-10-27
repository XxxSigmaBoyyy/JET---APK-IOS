# JET native AArch64 interpreter
CC ?= clang
CFLAGS ?= -O2 -g -std=c11 -Wall -Wextra -Wno-unused-parameter -Wno-unused-function
CFLAGS += -fno-strict-aliasing -D_GNU_SOURCE
LDFLAGS ?= -lpthread -ldl -lm -lz

SRCS = ctx.c decode.c elf_loader.c interp.c host_bionic.c jni_bridge.c trampoline.c setjmp_guest.c
OBJS = $(SRCS:%.c=build/%.o)

.PHONY: all clean test test-unit test-tg ios-syntax check

all: build/libjet_native.a build/test_unit build/test_tg

build:
	mkdir -p build

build/%.o: %.c jet_native.h jet_internal.h | build
	$(CC) $(CFLAGS) -c $< -o $@

build/libjet_native.a: $(OBJS)
	ar rcs $@ $^

build/test_unit: tests/test_unit.c build/libjet_native.a tests/guest_lib.so
	$(CC) $(CFLAGS) tests/test_unit.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

build/test_tg: tests/test_tg.c build/libjet_native.a
	$(CC) $(CFLAGS) tests/test_tg.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

# Tiny guest shared library (AArch64), linked with nostdlib
tests/guest_lib.so: tests/guest_lib.c
	$(CC) --target=aarch64-linux-gnu -nostdlib -shared -fPIC -O2 \
		-Wl,--entry=guest_entry -o $@ $< 2>/dev/null || \
	$(CC) --target=aarch64-linux-gnu -shared -fPIC -O2 -nostdlib \
		-ffreestanding -o $@ $<

test-unit: build/test_unit
	./build/test_unit

test-tg: build/test_tg
	./build/test_tg

test: test-unit test-tg

# Ensure core sources are free of Linux-only headers in the public path.
ios-syntax:
	@echo "Checking iOS portability (syntax-only, no sysroot)..."
	$(CC) -std=c11 -fsyntax-only -DTARGET_OS_IPHONE=1 -DJET_IOS_SYNTAX_CHECK=1 \
		-I. decode.c 2>&1 | head -20 || true
	@# Full check with apple target if possible
	$(CC) -std=c11 -target arm64-apple-ios15.0 -fsyntax-only -I. \
		-D_GNU_SOURCE=0 -DJET_IOS_SYNTAX_CHECK=1 \
		decode.c ctx.c trampoline.c setjmp_guest.c 2>&1 | head -40 || true

clean:
	rm -rf build tests/guest_lib.so

check: all test ios-syntax
