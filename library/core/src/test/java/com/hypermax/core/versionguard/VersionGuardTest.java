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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Covers the watermark decision logic on a plain JVM.
 *
 * <p>The production path uses Android Keystore; here the key provider is replaced by an in-memory
 * AES key, while framing, GCM authentication, payload encoding and the compare/update rules are the
 * real implementations. Android Keystore behaviour itself is only covered by an on-device
 * integration test, which this suite does not and cannot replace.</p>
 */
class VersionGuardTest {

    private static final String PACKAGE_NAME = "com.hypermax";

    @Test
    void firstLaunchCreatesWatermarkAndAllows() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");

        VersionGuardState.Allowed allowed = assertInstanceOf(
            VersionGuardState.Allowed.class, fixture.guard().checkNow());

        assertEquals(10L, allowed.currentVersionCode);
        assertEquals(1, fixture.store.writes);
        VersionWatermark watermark = fixture.storedWatermark();
        assertEquals(VersionWatermark.SCHEMA, watermark.schema);
        assertEquals(PACKAGE_NAME, watermark.packageName);
        assertEquals(10L, watermark.maxVersionCode);
        assertEquals("1.0.0-10", watermark.maxVersionName);
        assertTrue(watermark.updatedAt > 0L);
    }

    @Test
    void sameVersionDoesNotRewriteTheWatermark() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        VersionGuard guard = fixture.guard();
        assertInstanceOf(VersionGuardState.Allowed.class, guard.checkNow());
        byte[] firstPayload = fixture.store.payload.clone();

        assertInstanceOf(VersionGuardState.Allowed.class, guard.checkNow());

        assertEquals(1, fixture.store.writes);
        assertArrayEquals(firstPayload, fixture.store.payload);
    }

    @Test
    void upgradeRaisesTheRecordedVersion() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        VersionGuard guard = fixture.guard();
        assertInstanceOf(VersionGuardState.Allowed.class, guard.checkNow());

        fixture.versions.set(11L, "1.0.0-11");
        VersionGuardState.Allowed allowed = assertInstanceOf(
            VersionGuardState.Allowed.class, guard.checkNow());

        assertEquals(11L, allowed.currentVersionCode);
        assertEquals(2, fixture.store.writes);
        assertEquals(11L, fixture.storedWatermark().maxVersionCode);
    }

    @Test
    void downgradeIsBlockedAndKeepsTheHighestVersion() {
        Fixture fixture = new Fixture();
        fixture.versions.set(11L, "1.0.0-11");
        VersionGuard guard = fixture.guard();
        assertInstanceOf(VersionGuardState.Allowed.class, guard.checkNow());
        byte[] highestPayload = fixture.store.payload.clone();

        fixture.versions.set(10L, "1.0.0-10");
        VersionGuardState.Blocked blocked = assertInstanceOf(
            VersionGuardState.Blocked.class, guard.checkNow());

        assertEquals(BlockReason.DOWNGRADE, blocked.reason);
        assertEquals(10L, blocked.currentVersionCode);
        assertEquals(Long.valueOf(11L), blocked.requiredVersionCode);
        assertEquals(1, fixture.store.writes);
        assertArrayEquals(highestPayload, fixture.store.payload);
        assertEquals(11L, fixture.storedWatermark().maxVersionCode);
    }

    @Test
    void repeatedCheckDoesNotLiftADowngradeBlock() {
        Fixture fixture = new Fixture();
        fixture.versions.set(9L, "1.0.0-9");
        VersionGuard guard = fixture.guard();
        assertInstanceOf(VersionGuardState.Allowed.class, guard.checkNow());

        fixture.versions.set(8L, "1.0.0-8");
        assertInstanceOf(VersionGuardState.Blocked.class, guard.checkNow());
        assertInstanceOf(VersionGuardState.Blocked.class, guard.checkNow());

        fixture.versions.set(10L, "1.0.0-10");
        assertInstanceOf(VersionGuardState.Allowed.class, guard.checkNow());
        assertEquals(10L, fixture.storedWatermark().maxVersionCode);
    }

    @Test
    void tamperedCiphertextIsBlocked() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        assertInstanceOf(VersionGuardState.Allowed.class, fixture.guard().checkNow());

        byte[] tampered = fixture.store.payload.clone();
        tampered[tampered.length - 1] ^= 0x01;
        fixture.store.payload = tampered;

        VersionGuardState.Blocked blocked = assertInstanceOf(
            VersionGuardState.Blocked.class, fixture.guard().checkNow());
        assertEquals(BlockReason.WATERMARK_CORRUPT, blocked.reason);
        assertNull(blocked.requiredVersionCode);
        assertEquals(1, fixture.store.writes);
    }

    @Test
    void truncatedWatermarkIsBlocked() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        assertInstanceOf(VersionGuardState.Allowed.class, fixture.guard().checkNow());

        byte[] truncated = new byte[20];
        System.arraycopy(fixture.store.payload, 0, truncated, 0, truncated.length);
        fixture.store.payload = truncated;

        assertBlocked(fixture.guard(), BlockReason.WATERMARK_CORRUPT);
    }

    @Test
    void emptyWatermarkFileIsBlockedInsteadOfReinitialized() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        fixture.store.payload = new byte[0];

        assertBlocked(fixture.guard(), BlockReason.WATERMARK_CORRUPT);
        assertEquals(0, fixture.store.writes);
    }

    @Test
    void invalidMagicIsBlocked() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        assertInstanceOf(VersionGuardState.Allowed.class, fixture.guard().checkNow());

        byte[] broken = fixture.store.payload.clone();
        broken[0] = 'X';
        fixture.store.payload = broken;

        assertBlocked(fixture.guard(), BlockReason.WATERMARK_CORRUPT);
    }

    @Test
    void watermarkBoundToAnotherPackageIsBlocked() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        fixture.store.payload = fixture.cipher()
            .encrypt(VersionWatermark.create("com.example.other", 10L, "1.0.0", 1L).encode());

        assertBlocked(fixture.guard(), BlockReason.WATERMARK_CORRUPT);
    }

    @Test
    void watermarkEncryptedForAnotherPackageIsBlocked() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        FramedAesGcmCipher foreignCipher = new FramedAesGcmCipher("com.example.other", () -> fixture.key);
        fixture.store.payload = foreignCipher
            .encrypt(VersionWatermark.create(PACKAGE_NAME, 10L, "1.0.0", 1L).encode());

        assertBlocked(fixture.guard(), BlockReason.WATERMARK_CORRUPT);
    }

    @Test
    void unsupportedSchemaIsBlocked() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        fixture.store.payload = fixture.cipher().encrypt(
            payloadWithSchema(VersionWatermark.SCHEMA + 1, PACKAGE_NAME, 10L));

        assertBlocked(fixture.guard(), BlockReason.WATERMARK_CORRUPT);
    }

    @Test
    void unusableKeyBlocksInsteadOfFallingBackToPlaintext() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        fixture.store.payload = fixture.cipher()
            .encrypt(VersionWatermark.create(PACKAGE_NAME, 10L, "1.0.0", 1L).encode());

        assertBlocked(fixture.guardWithCipher(new UnavailableCipher()),
            BlockReason.WATERMARK_READ_FAILED);
    }

    @Test
    void unreadableFileIsNotTreatedAsFirstInstall() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        fixture.store.failRead = true;

        assertBlocked(fixture.guard(), BlockReason.WATERMARK_READ_FAILED);
        assertEquals(0, fixture.store.writes);
    }

    @Test
    void unreadableInstalledVersionBlocks() {
        Fixture fixture = new Fixture();
        fixture.versions.failure = new IllegalStateException("package manager unavailable");

        VersionGuardState.Blocked blocked = assertBlocked(
            fixture.guard(), BlockReason.WATERMARK_READ_FAILED);
        assertEquals(VersionGuard.UNKNOWN_VERSION_CODE, blocked.currentVersionCode);
        assertEquals(0, fixture.store.writes);
    }

    @Test
    void failedInitialWriteBlocks() {
        Fixture fixture = new Fixture();
        fixture.versions.set(10L, "1.0.0-10");
        fixture.store.failWrite = true;

        assertBlocked(fixture.guard(), BlockReason.WATERMARK_WRITE_FAILED);
        assertNull(fixture.store.payload);
    }

    @Test
    void failedUpgradeWriteBlocksAndKeepsTheOldWatermark() {
        Fixture fixture = new Fixture();
        fixture.versions.set(3L, "1.0.0-3");
        VersionGuard guard = fixture.guard();
        assertInstanceOf(VersionGuardState.Allowed.class, guard.checkNow());
        byte[] previous = fixture.store.payload.clone();

        fixture.versions.set(4L, "1.0.0-4");
        fixture.store.failWrite = true;

        assertBlocked(guard, BlockReason.WATERMARK_WRITE_FAILED);
        assertArrayEquals(previous, fixture.store.payload);
        assertEquals(3L, fixture.storedWatermark().maxVersionCode);
    }

    @Test
    void clearedDataInitializesAgain() {
        Fixture fixture = new Fixture();
        fixture.versions.set(11L, "1.0.0-11");
        assertInstanceOf(VersionGuardState.Allowed.class, fixture.guard().checkNow());
        fixture.store.payload = null;
        fixture.store.writes = 0;

        fixture.versions.set(4L, "1.0.0-4");
        assertInstanceOf(VersionGuardState.Allowed.class, fixture.guard().checkNow());
        assertEquals(1, fixture.store.writes);
        assertEquals(4L, fixture.storedWatermark().maxVersionCode);
    }

    @Test
    void concurrentChecksKeepOneWatermark() throws InterruptedException, ExecutionException {
        Fixture fixture = new Fixture();
        fixture.versions.set(7L, "1.0.0-7");
        VersionGuard guard = fixture.guard();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<VersionGuardState>> results = new ArrayList<>();
        for (int index = 0; index < threads; index++) {
            results.add(pool.submit(() -> {
                start.await();
                return guard.checkNow();
            }));
        }
        start.countDown();
        for (Future<VersionGuardState> result : results) {
            assertInstanceOf(VersionGuardState.Allowed.class, result.get());
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10L, TimeUnit.SECONDS));

        assertEquals(1, fixture.store.writes);
        assertEquals(7L, fixture.storedWatermark().maxVersionCode);
    }

    @Test
    void backgroundCheckNotifiesTheWaiter() throws InterruptedException {
        Fixture fixture = new Fixture();
        fixture.versions.set(12L, "1.0.0-12");
        VersionGuard guard = fixture.guard();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<VersionGuardState> received = new AtomicReference<>();

        guard.requestCheck();
        guard.awaitState(state -> {
            received.set(state);
            latch.countDown();
        });

        assertTrue(latch.await(10L, TimeUnit.SECONDS));
        assertInstanceOf(VersionGuardState.Allowed.class, received.get());
        assertEquals(12L, fixture.storedWatermark().maxVersionCode);
    }

    @Test
    void backgroundCheckDoesNotResetACommittedBlock() {
        Fixture fixture = new Fixture();
        fixture.versions.set(9L, "1.0.0-9");
        VersionGuard guard = fixture.guard();
        assertInstanceOf(VersionGuardState.Allowed.class, guard.checkNow());

        fixture.versions.set(8L, "1.0.0-8");
        assertInstanceOf(VersionGuardState.Blocked.class, guard.checkNow());

        guard.requestCheck();

        assertInstanceOf(VersionGuardState.Blocked.class, guard.state());
    }

    private static VersionGuardState.Blocked assertBlocked(VersionGuard guard, BlockReason reason) {
        VersionGuardState.Blocked blocked = assertInstanceOf(
            VersionGuardState.Blocked.class, guard.checkNow());
        assertEquals(reason, blocked.reason);
        return blocked;
    }

    private static byte[] payloadWithSchema(int schema, String packageName, long versionCode) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeInt(schema);
            out.writeUTF(packageName);
            out.writeLong(versionCode);
            out.writeUTF("1.0.0");
            out.writeLong(1L);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to build the test payload", error);
        }
        return buffer.toByteArray();
    }

    private static SecretKey generateKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (java.security.GeneralSecurityException error) {
            throw new IllegalStateException("Unable to generate the test key", error);
        }
    }

    private static final class Fixture {

        private final MutableVersionSource versions = new MutableVersionSource();
        private final InMemoryStore store = new InMemoryStore();
        private final SecretKey key = generateKey();

        private FramedAesGcmCipher cipher() {
            return new FramedAesGcmCipher(PACKAGE_NAME, () -> key);
        }

        private VersionGuard guard() {
            return guardWithCipher(cipher());
        }

        private VersionGuard guardWithCipher(WatermarkCipher cipher) {
            return new VersionGuard(PACKAGE_NAME, versions, cipher, store, Runnable::run, null);
        }

        private VersionWatermark storedWatermark() {
            return VersionWatermark.decode(cipher().decrypt(store.payload));
        }
    }

    private static final class MutableVersionSource implements VersionGuard.PackageVersionSource {

        private long versionCode = 1L;
        private String versionName = "1.0.0";
        private RuntimeException failure;

        private void set(long versionCode, String versionName) {
            this.versionCode = versionCode;
            this.versionName = versionName;
        }

        @Override
        public VersionGuard.PackageVersion readInstalledVersion() {
            if (failure != null) {
                throw failure;
            }
            return new VersionGuard.PackageVersion(versionCode, versionName);
        }
    }

    private static final class InMemoryStore implements WatermarkStore {

        private byte[] payload;
        private int writes;
        private boolean failRead;
        private boolean failWrite;

        @Override
        public byte[] read() throws IOException {
            if (failRead) {
                throw new IOException("simulated read failure");
            }
            return payload == null ? null : payload.clone();
        }

        @Override
        public void write(byte[] newPayload) throws IOException {
            if (failWrite) {
                throw new IOException("simulated write failure");
            }
            writes++;
            payload = newPayload.clone();
        }
    }

    private static final class UnavailableCipher implements WatermarkCipher {

        @Override
        public byte[] encrypt(byte[] plaintext) {
            throw new WatermarkUnavailableException("simulated keystore failure");
        }

        @Override
        public byte[] decrypt(byte[] payload) {
            throw new WatermarkUnavailableException("simulated keystore failure");
        }
    }
}
