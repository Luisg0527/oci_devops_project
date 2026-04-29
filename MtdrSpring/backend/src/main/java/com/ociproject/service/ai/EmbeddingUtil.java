package com.ociproject.service.ai;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class EmbeddingUtil {

    private EmbeddingUtil() {}

    public static byte[] floatsToBytes(float[] vec) {
        ByteBuffer buf = ByteBuffer.allocate(vec.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : vec) buf.putFloat(v);
        return buf.array();
    }

    public static float[] bytesToFloats(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] vec = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < vec.length; i++) vec[i] = buf.getFloat();
        return vec;
    }

    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("vector length mismatch: " + a.length + " vs " + b.length);
        }
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na  += a[i] * a[i];
            nb  += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom == 0 ? 0.0 : dot / denom;
    }
}
