/*
 * This file is part of HyperMax.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.hypermax.core.versionguard;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Plaintext payload of the local version watermark.
 *
 * <p>The payload is only meaningful after the {@link FramedAesGcmCipher} authenticated it, so the
 * decoder treats any structural deviation as corruption instead of guessing a default.</p>
 */
public final class VersionWatermark {

    /** Watermark format version carried inside the encrypted payload. */
    public static final int SCHEMA = 1;

    public final int schema;
    public final String packageName;
    /** Highest versionCode this app has run with. Only this field is used for comparisons. */
    public final long maxVersionCode;
    /** Diagnostic only; never compared or sorted. */
    public final String maxVersionName;
    /** Diagnostic only. */
    public final long updatedAt;

    private VersionWatermark(int schema, @NonNull String packageName, long maxVersionCode,
                             @NonNull String maxVersionName, long updatedAt) {
        this.schema = schema;
        this.packageName = packageName;
        this.maxVersionCode = maxVersionCode;
        this.maxVersionName = maxVersionName;
        this.updatedAt = updatedAt;
    }

    public static VersionWatermark create(@NonNull String packageName, long versionCode,
                                          String versionName, long updatedAt) {
        return new VersionWatermark(
            SCHEMA,
            packageName,
            versionCode,
            versionName == null ? "" : versionName,
            updatedAt
        );
    }

    public byte[] encode() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeInt(schema);
            out.writeUTF(packageName);
            out.writeLong(maxVersionCode);
            out.writeUTF(maxVersionName);
            out.writeLong(updatedAt);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to encode the version watermark", error);
        }
        return buffer.toByteArray();
    }

    public static VersionWatermark decode(byte[] payload) {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
        try {
            int schema = in.readInt();
            if (schema != SCHEMA) {
                throw new WatermarkCorruptException("Unsupported watermark schema " + schema);
            }
            String packageName = in.readUTF();
            long maxVersionCode = in.readLong();
            String maxVersionName = in.readUTF();
            long updatedAt = in.readLong();
            if (in.available() != 0) {
                throw new WatermarkCorruptException("Trailing bytes after the watermark payload");
            }
            if (maxVersionCode <= 0) {
                throw new WatermarkCorruptException("Invalid maxVersionCode " + maxVersionCode);
            }
            return new VersionWatermark(schema, packageName, maxVersionCode, maxVersionName, updatedAt);
        } catch (IOException error) {
            throw new WatermarkCorruptException("Unable to decode the version watermark", error);
        }
    }

    /**
     * Rejects a watermark that was created by another app, which can happen when the encrypted file
     * is copied around or when the package name changed.
     */
    public void requireCompatibleWith(@NonNull String currentPackageName) {
        if (!packageName.equals(currentPackageName)) {
            throw new WatermarkCorruptException("Watermark belongs to " + packageName);
        }
    }

    @Override
    public String toString() {
        return "VersionWatermark{schema=" + schema +
            ", packageName=" + packageName +
            ", maxVersionCode=" + maxVersionCode +
            ", maxVersionName=" + maxVersionName +
            ", updatedAt=" + updatedAt + '}';
    }
}
