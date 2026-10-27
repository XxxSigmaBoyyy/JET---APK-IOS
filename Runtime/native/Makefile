# JET native AArch64 interpreter
CC ?= clang
CFLAGS ?= -O2 -g -std=c11 -Wall -Wextra -Wno-unused-parameter -Wno-unused-function
CFLAGS += -fno-strict-aliasing -D_GNU_SOURCE -Ithird_party
LDFLAGS ?= -lpthread -ldl -lm -Lthird_party -lz

SRCS = ctx.c decode.c elf_loader.c interp.c host_bionic.c jni_bridge.c trampoline.c setjmp_guest.c asimd.c
OBJS = $(SRCS:%.c=build/%.o)

.PHONY: all clean test test-unit test-tg test-jni ios-syntax check

all: build/libjet_native.a build/test_unit build/test_tg build/test_jni build/decode_sweep

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

build/test_jni: tests/test_jni.c build/libjet_native.a
	$(CC) $(CFLAGS) tests/test_jni.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

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

test-jni: build/test_jni
	./build/test_jni

build/decode_sweep: tests/decode_sweep.c build/libjet_native.a
	$(CC) $(CFLAGS) tests/decode_sweep.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

test: test-unit test-tg test-jni


# Ensure core sources are free of Linux-only headers in the public path.
ios-syntax:
	@echo "Checking iOS portability (syntax-only)..."
	@for f in $(SRCS); do \
	  echo "---- $$f"; \
	  $(CC) -std=c11 -Wall -Wextra -Wno-unused-parameter -target arm64-apple-ios15.0 \
	    -fsyntax-only -I. -Ithird_party -DJET_IOS_SYNTAX_CHECK=1 \
	    -DTARGET_OS_IPHONE=1 -D__APPLE__=1 $$f 2>&1 | head -30 || true; \
	done

clean:
	rm -rf build tests/guest_lib.so

check: all test ios-syntax
