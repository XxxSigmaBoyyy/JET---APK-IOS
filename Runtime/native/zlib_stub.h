/* Minimal zlib stub for environments without zlib headers. */
#ifndef ZLIB_STUB_H
#define ZLIB_STUB_H
typedef struct { void *next_in; unsigned avail_in; unsigned long total_in;
                 void *next_out; unsigned avail_out; unsigned long total_out;
                 char *msg; void *state; void *zalloc; void *zfree; void *opaque;
                 int data_type; unsigned long adler; unsigned long reserved; } z_stream;
typedef z_stream *z_streamp;
static inline int inflateInit2_(z_streamp s, int w, const char *v, int n){ (void)s;(void)w;(void)v;(void)n; return -2; }
static inline int inflate(z_streamp s, int f){ (void)s;(void)f; return -2; }
static inline int inflateEnd(z_streamp s){ (void)s; return -2; }
static inline int deflateInit2_(z_streamp s, int a, int b, int c, int d, int e, const char *v, int n){ (void)s;(void)a;(void)b;(void)c;(void)d;(void)e;(void)v;(void)n; return -2; }
static inline int deflate(z_streamp s, int f){ (void)s;(void)f; return -2; }
static inline int deflateEnd(z_streamp s){ (void)s; return -2; }
#endif
