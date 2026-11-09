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

import android.util.AtomicFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Stores the sealed watermark in the app's no-backup directory through {@link AtomicFile}.
 *
 * <p>{@link AtomicFile} writes a side file and only renames it over the real file once the content
 * is complete, so a crash mid-write leaves the previous watermark intact. The file is never deleted
 * before the new content is fully written.</p>
 */
public final class AtomicFileWatermarkStore implements WatermarkStore {

    private final File file;
    private final AtomicFile atomicFile;

    public AtomicFileWatermarkStore(File file) {
        this.file = file;
        this.atomicFile = new AtomicFile(file);
    }

    @Override
    public byte[] read() throws IOException {
        if (!file.exists()) {
            return null;
        }
        try (FileInputStream in = atomicFile.openRead()) {
            return in.readAllBytes();
        }
    }

    @Override
    public void write(byte[] payload) throws IOException {
        FileOutputStream out = null;
        try {
            out = atomicFile.startWrite();
            out.write(payload);
            atomicFile.finishWrite(out);
        } catch (IOException error) {
            if (out != null) {
                atomicFile.failWrite(out);
            }
            throw error;
        }
    }
}
