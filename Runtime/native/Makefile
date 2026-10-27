# JET native AArch64 interpreter
CC ?= clang
CFLAGS ?= -O2 -g -std=c11 -Wall -Wextra -Wno-unused-parameter -Wno-unused-function
CFLAGS += -fno-strict-aliasing -D_GNU_SOURCE -Ithird_party
LDFLAGS ?= -lpthread -ldl -lm -Lthird_party -lz

SRCS = ctx.c decode.c elf_loader.c interp.c host_bionic.c guest_abi.c darwin_abi.c epoll_kq.c netdb_abi.c jni_bridge.c trampoline.c setjmp_guest.c asimd.c
OBJS = $(SRCS:%.c=build/%.o)

.PHONY: all clean test test-unit test-tg test-jni ios-syntax check

all: build/libjet_native.a build/test_unit build/test_tg build/test_jni build/test_mem_bitfield build/test_v9_jni_dl build/test_cxx_eh build/test_v10_trap_abi build/test_v101_guest_abi build/test_v101_darwin_abi build/test_v11_xzr_excl build/test_v102_fs_netdb build/decode_sweep

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

build/test_mem_bitfield: tests/test_mem_bitfield.c build/libjet_native.a
	$(CC) $(CFLAGS) tests/test_mem_bitfield.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

build/test_v9_jni_dl: tests/test_v9_jni_dl.c build/libjet_native.a
	$(CC) $(CFLAGS) tests/test_v9_jni_dl.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

build/test_cxx_eh: tests/test_cxx_eh.c build/libjet_native.a tests/guest_lib.so
	$(CC) $(CFLAGS) tests/test_cxx_eh.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

build/test_v10_trap_abi: tests/test_v10_trap_abi.c build/libjet_native.a tests/guest_lib.so
	$(CC) $(CFLAGS) tests/test_v10_trap_abi.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

build/test_v101_guest_abi: tests/test_v101_guest_abi.c build/libjet_native.a tests/guest_lib.so
	$(CC) $(CFLAGS) tests/test_v101_guest_abi.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

build/test_v102_fs_netdb: tests/test_v102_fs_netdb.c build/libjet_native.a tests/guest_lib.so
	$(CC) $(CFLAGS) tests/test_v102_fs_netdb.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

test-v102: build/test_v102_fs_netdb
	./build/test_v102_fs_netdb

build/test_v101_darwin_abi: tests/test_v101_darwin_abi.c build/libjet_native.a tests/guest_lib.so
	$(CC) $(CFLAGS) tests/test_v101_darwin_abi.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

build/test_v11_xzr_excl: tests/test_v11_xzr_excl.c build/libjet_native.a tests/guest_lib.so
	$(CC) $(CFLAGS) tests/test_v11_xzr_excl.c -I. -Lbuild -ljet_native $(LDFLAGS) -o $@

test-mem: build/test_mem_bitfield
	./build/test_mem_bitfield

test-v9: build/test_v9_jni_dl build/test_cxx_eh
	./build/test_v9_jni_dl
	./build/test_cxx_eh

test: test-unit test-tg test-jni test-mem test-v9 test-v101

test-v101: build/test_v101_guest_abi build/test_v101_darwin_abi
	./build/test_v101_guest_abi
	./build/test_v101_darwin_abi

test-v10: build/test_v10_trap_abi
	./build/test_v10_trap_abi


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
