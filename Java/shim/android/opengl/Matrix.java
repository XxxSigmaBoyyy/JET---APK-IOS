package android.opengl;
/** AOSP android.opengl.Matrix: column-major 4x4 float math (pure Java, identical formulas). */
public class Matrix {
    @Deprecated public Matrix() {}
    public static void multiplyMM(float[] r, int ro, float[] lhs, int lo, float[] rhs, int rgo) {
        if (r == null) throw new IllegalArgumentException("result == null"); if (lhs == null) throw new IllegalArgumentException("lhs == null"); if (rhs == null) throw new IllegalArgumentException("rhs == null");
        if (ro + 16 > r.length) throw new IllegalArgumentException("resultOffset + 16 > result.length"); if (lo + 16 > lhs.length) throw new IllegalArgumentException("lhsOffset + 16 > lhs.length"); if (rgo + 16 > rhs.length) throw new IllegalArgumentException("rhsOffset + 16 > rhs.length");
        float[] t = new float[16];
        for (int i = 0; i < 4; i++) { final float r0 = rhs[rgo + i * 4], r1 = rhs[rgo + i * 4 + 1], r2 = rhs[rgo + i * 4 + 2], r3 = rhs[rgo + i * 4 + 3];
            for (int j = 0; j < 4; j++) t[i * 4 + j] = lhs[lo + j] * r0 + lhs[lo + 4 + j] * r1 + lhs[lo + 8 + j] * r2 + lhs[lo + 12 + j] * r3; }
        System.arraycopy(t, 0, r, ro, 16);
    }
    public static void multiplyMV(float[] r, int ro, float[] lhs, int lo, float[] v, int vo) {
        float x = v[vo], y = v[vo + 1], z = v[vo + 2], w = v[vo + 3]; float[] t = new float[4];
        for (int j = 0; j < 4; j++) t[j] = lhs[lo + j] * x + lhs[lo + 4 + j] * y + lhs[lo + 8 + j] * z + lhs[lo + 12 + j] * w;
        System.arraycopy(t, 0, r, ro, 4);
    }
    public static void transposeM(float[] mTrans, int mTransOffset, float[] m, int mOffset) {
        for (int i = 0; i < 4; i++) { int mBase = i * 4 + mOffset; mTrans[i + mTransOffset] = m[mBase]; mTrans[i + 4 + mTransOffset] = m[mBase + 1]; mTrans[i + 8 + mTransOffset] = m[mBase + 2]; mTrans[i + 12 + mTransOffset] = m[mBase + 3]; }
    }
    public static boolean invertM(float[] mInv, int mInvOffset, float[] m, int mOffset) {
        final float src0 = m[mOffset], src4 = m[mOffset + 1], src8 = m[mOffset + 2], src12 = m[mOffset + 3], src1 = m[mOffset + 4], src5 = m[mOffset + 5], src9 = m[mOffset + 6], src13 = m[mOffset + 7],
            src2 = m[mOffset + 8], src6 = m[mOffset + 9], src10 = m[mOffset + 10], src14 = m[mOffset + 11], src3 = m[mOffset + 12], src7 = m[mOffset + 13], src11 = m[mOffset + 14], src15 = m[mOffset + 15];
        final float atmp0 = src10 * src15, atmp1 = src11 * src14, atmp2 = src9 * src15, atmp3 = src11 * src13, atmp4 = src9 * src14, atmp5 = src10 * src13, atmp6 = src8 * src15, atmp7 = src11 * src12, atmp8 = src8 * src14, atmp9 = src10 * src12, atmp10 = src8 * src13, atmp11 = src9 * src12;
        final float dst0 = (atmp0 * src5 + atmp3 * src6 + atmp4 * src7) - (atmp1 * src5 + atmp2 * src6 + atmp5 * src7);
        final float dst1 = (atmp1 * src4 + atmp6 * src6 + atmp9 * src7) - (atmp0 * src4 + atmp7 * src6 + atmp8 * src7);
        final float dst2 = (atmp2 * src4 + atmp7 * src5 + atmp10 * src7) - (atmp3 * src4 + atmp6 * src5 + atmp11 * src7);
        final float dst3 = (atmp5 * src4 + atmp8 * src5 + atmp11 * src6) - (atmp4 * src4 + atmp9 * src5 + atmp10 * src6);
        final float dst4 = (atmp1 * src1 + atmp2 * src2 + atmp5 * src3) - (atmp0 * src1 + atmp3 * src2 + atmp4 * src3);
        final float dst5 = (atmp0 * src0 + atmp7 * src2 + atmp8 * src3) - (atmp1 * src0 + atmp6 * src2 + atmp9 * src3);
        final float dst6 = (atmp3 * src0 + atmp6 * src1 + atmp11 * src3) - (atmp2 * src0 + atmp7 * src1 + atmp10 * src3);
        final float dst7 = (atmp4 * src0 + atmp9 * src1 + atmp10 * src2) - (atmp5 * src0 + atmp8 * src1 + atmp11 * src2);
        final float btmp0 = src2 * src7, btmp1 = src3 * src6, btmp2 = src1 * src7, btmp3 = src3 * src5, btmp4 = src1 * src6, btmp5 = src2 * src5, btmp6 = src0 * src7, btmp7 = src3 * src4, btmp8 = src0 * src6, btmp9 = src2 * src4, btmp10 = src0 * src5, btmp11 = src1 * src4;
        final float dst8 = (btmp0 * src13 + btmp3 * src14 + btmp4 * src15) - (btmp1 * src13 + btmp2 * src14 + btmp5 * src15);
        final float dst9 = (btmp1 * src12 + btmp6 * src14 + btmp9 * src15) - (btmp0 * src12 + btmp7 * src14 + btmp8 * src15);
        final float dst10 = (btmp2 * src12 + btmp7 * src13 + btmp10 * src15) - (btmp3 * src12 + btmp6 * src13 + btmp11 * src15);
        final float dst11 = (btmp5 * src12 + btmp8 * src13 + btmp11 * src14) - (btmp4 * src12 + btmp9 * src13 + btmp10 * src14);
        final float dst12 = (btmp2 * src10 + btmp5 * src11 + btmp1 * src9) - (btmp4 * src11 + btmp0 * src9 + btmp3 * src10);
        final float dst13 = (btmp8 * src11 + btmp0 * src8 + btmp7 * src10) - (btmp6 * src10 + btmp9 * src11 + btmp1 * src8);
        final float dst14 = (btmp6 * src9 + btmp11 * src11 + btmp3 * src8) - (btmp10 * src11 + btmp2 * src8 + btmp7 * src9);
        final float dst15 = (btmp10 * src10 + btmp4 * src8 + btmp9 * src9) - (btmp8 * src9 + btmp11 * src10 + btmp5 * src8);
        final float det = src0 * dst0 + src1 * dst1 + src2 * dst2 + src3 * dst3;
        if (det == 0.0f) return false;
        final float invdet = 1.0f / det;
        float[] d = {dst0, dst1, dst2, dst3, dst4, dst5, dst6, dst7, dst8, dst9, dst10, dst11, dst12, dst13, dst14, dst15};
        for (int k = 0; k < 16; k++) mInv[k + mInvOffset] = d[k] * invdet;
        return true;
    }
    public static void orthoM(float[] m, int o, float left, float right, float bottom, float top, float near, float far) {
        if (left == right) throw new IllegalArgumentException("left == right"); if (bottom == top) throw new IllegalArgumentException("bottom == top"); if (near == far) throw new IllegalArgumentException("near == far");
        final float rw = 1.0f / (right - left), rh = 1.0f / (top - bottom), rd = 1.0f / (far - near);
        java.util.Arrays.fill(m, o, o + 16, 0f);
        m[o] = 2.0f * rw; m[o + 5] = 2.0f * rh; m[o + 10] = -2.0f * rd; m[o + 12] = -(right + left) * rw; m[o + 13] = -(top + bottom) * rh; m[o + 14] = -(far + near) * rd; m[o + 15] = 1.0f;
    }
    public static void frustumM(float[] m, int o, float left, float right, float bottom, float top, float near, float far) {
        if (left == right) throw new IllegalArgumentException("left == right"); if (top == bottom) throw new IllegalArgumentException("top == bottom"); if (near == far) throw new IllegalArgumentException("near == far");
        if (near <= 0.0f) throw new IllegalArgumentException("near <= 0.0f"); if (far <= 0.0f) throw new IllegalArgumentException("far <= 0.0f");
        final float rw = 1.0f / (right - left), rh = 1.0f / (top - bottom), rd = 1.0f / (near - far);
        java.util.Arrays.fill(m, o, o + 16, 0f);
        m[o] = 2.0f * (near * rw); m[o + 5] = 2.0f * (near * rh); m[o + 8] = (right + left) * rw; m[o + 9] = (top + bottom) * rh; m[o + 10] = (far + near) * rd; m[o + 11] = -1.0f; m[o + 14] = 2.0f * (far * near * rd);
    }
    public static void perspectiveM(float[] m, int o, float fovy, float aspect, float zNear, float zFar) {
        float f = 1.0f / (float) Math.tan(fovy * (Math.PI / 360.0)); float rangeReciprocal = 1.0f / (zNear - zFar);
        java.util.Arrays.fill(m, o, o + 16, 0f);
        m[o] = f / aspect; m[o + 5] = f; m[o + 10] = (zFar + zNear) * rangeReciprocal; m[o + 11] = -1.0f; m[o + 14] = 2.0f * zFar * zNear * rangeReciprocal;
    }
    public static float length(float x, float y, float z) { return (float) Math.sqrt(x * x + y * y + z * z); }
    public static void setIdentityM(float[] sm, int o) { for (int i = 0; i < 16; i++) sm[o + i] = 0; for (int i = 0; i < 16; i += 5) sm[o + i] = 1.0f; }
    public static void scaleM(float[] sm, int so, float[] m, int mo, float x, float y, float z) { for (int i = 0; i < 4; i++) { int smi = so + i, mi = mo + i; sm[smi] = m[mi] * x; sm[4 + smi] = m[4 + mi] * y; sm[8 + smi] = m[8 + mi] * z; sm[12 + smi] = m[12 + mi]; } }
    public static void scaleM(float[] m, int mo, float x, float y, float z) { for (int i = 0; i < 4; i++) { int mi = mo + i; m[mi] *= x; m[4 + mi] *= y; m[8 + mi] *= z; } }
    public static void translateM(float[] tm, int tmo, float[] m, int mo, float x, float y, float z) { for (int i = 0; i < 12; i++) tm[tmo + i] = m[mo + i]; for (int i = 0; i < 4; i++) { int tmi = tmo + i, mi = mo + i; tm[12 + tmi] = m[mi] * x + m[4 + mi] * y + m[8 + mi] * z + m[12 + mi]; } }
    public static void translateM(float[] m, int mo, float x, float y, float z) { for (int i = 0; i < 4; i++) { int mi = mo + i; m[12 + mi] += m[mi] * x + m[4 + mi] * y + m[8 + mi] * z; } }
    public static void rotateM(float[] rm, int rmo, float[] m, int mo, float a, float x, float y, float z) { float[] t = new float[16]; setRotateM(t, 0, a, x, y, z); multiplyMM(rm, rmo, m, mo, t, 0); }
    public static void rotateM(float[] m, int mo, float a, float x, float y, float z) { float[] t = new float[32]; setRotateM(t, 0, a, x, y, z); multiplyMM(t, 16, m, mo, t, 0); System.arraycopy(t, 16, m, mo, 16); }
    public static void setRotateM(float[] rm, int o, float a, float x, float y, float z) {
        rm[o + 3] = 0; rm[o + 7] = 0; rm[o + 11] = 0; rm[o + 12] = 0; rm[o + 13] = 0; rm[o + 14] = 0; rm[o + 15] = 1;
        a *= (float) (Math.PI / 180.0f); float s = (float) Math.sin(a), c = (float) Math.cos(a);
        if (1.0f == x && 0.0f == y && 0.0f == z) { rm[o + 5] = c; rm[o + 10] = c; rm[o + 6] = s; rm[o + 9] = -s; rm[o + 1] = 0; rm[o + 2] = 0; rm[o + 4] = 0; rm[o + 8] = 0; rm[o] = 1; }
        else if (0.0f == x && 1.0f == y && 0.0f == z) { rm[o] = c; rm[o + 10] = c; rm[o + 8] = s; rm[o + 2] = -s; rm[o + 1] = 0; rm[o + 4] = 0; rm[o + 6] = 0; rm[o + 9] = 0; rm[o + 5] = 1; }
        else if (0.0f == x && 0.0f == y && 1.0f == z) { rm[o] = c; rm[o + 5] = c; rm[o + 1] = s; rm[o + 4] = -s; rm[o + 2] = 0; rm[o + 6] = 0; rm[o + 8] = 0; rm[o + 9] = 0; rm[o + 10] = 1; }
        else { float len = length(x, y, z); if (1.0f != len) { float recipLen = 1.0f / len; x *= recipLen; y *= recipLen; z *= recipLen; }
            float nc = 1.0f - c, xy = x * y, yz = y * z, zx = z * x, xs = x * s, ys = y * s, zs = z * s;
            rm[o] = x * x * nc + c; rm[o + 4] = xy * nc - zs; rm[o + 8] = zx * nc + ys; rm[o + 1] = xy * nc + zs; rm[o + 5] = y * y * nc + c; rm[o + 9] = yz * nc - xs; rm[o + 2] = zx * nc - ys; rm[o + 6] = yz * nc + xs; rm[o + 10] = z * z * nc + c; }
    }
    public static void setRotateEulerM(float[] rm, int o, float x, float y, float z) {
        x *= (float) (Math.PI / 180.0f); y *= (float) (Math.PI / 180.0f); z *= (float) (Math.PI / 180.0f);
        float cx = (float) Math.cos(x), sx = (float) Math.sin(x), cy = (float) Math.cos(y), sy = (float) Math.sin(y), cz = (float) Math.cos(z), sz = (float) Math.sin(z), cxsy = cx * sy, sxsy = sx * sy;
        rm[o] = cy * cz; rm[o + 1] = -cy * sz; rm[o + 2] = sy; rm[o + 3] = 0.0f; rm[o + 4] = cxsy * cz + cx * sz; rm[o + 5] = -cxsy * sz + cx * cz; rm[o + 6] = -sx * cy; rm[o + 7] = 0.0f;
        rm[o + 8] = -sxsy * cz + sx * sz; rm[o + 9] = sxsy * sz + sx * cz; rm[o + 10] = cx * cy; rm[o + 11] = 0.0f; rm[o + 12] = 0.0f; rm[o + 13] = 0.0f; rm[o + 14] = 0.0f; rm[o + 15] = 1.0f;
    }
    public static void setRotateEulerM2(float[] rm, int o, float x, float y, float z) { setRotateEulerM(rm, o, x, y, z); }
    public static void setLookAtM(float[] rm, int o, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        float fx = centerX - eyeX, fy = centerY - eyeY, fz = centerZ - eyeZ; float rlf = 1.0f / length(fx, fy, fz); fx *= rlf; fy *= rlf; fz *= rlf;
        float sx = fy * upZ - fz * upY, sy = fz * upX - fx * upZ, sz = fx * upY - fy * upX; float rls = 1.0f / length(sx, sy, sz); sx *= rls; sy *= rls; sz *= rls;
        float ux = sy * fz - sz * fy, uy = sz * fx - sx * fz, uz = sx * fy - sy * fx;
        rm[o] = sx; rm[o + 1] = ux; rm[o + 2] = -fx; rm[o + 3] = 0.0f; rm[o + 4] = sy; rm[o + 5] = uy; rm[o + 6] = -fy; rm[o + 7] = 0.0f; rm[o + 8] = sz; rm[o + 9] = uz; rm[o + 10] = -fz; rm[o + 11] = 0.0f;
        rm[o + 12] = 0.0f; rm[o + 13] = 0.0f; rm[o + 14] = 0.0f; rm[o + 15] = 1.0f;
        translateM(rm, o, -eyeX, -eyeY, -eyeZ);
    }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
