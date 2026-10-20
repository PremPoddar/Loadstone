package com.prempoddar.loadstone.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.prempoddar.loadstone.data.db.LoadstoneDatabase
import com.prempoddar.loadstone.data.photo.PhotoStore
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinBackup
import com.prempoddar.loadstone.data.pin.PinRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class PinBackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun repo() = PinRepository(Room.inMemoryDatabaseBuilder(context, LoadstoneDatabase::class.java).allowMainThreadQueries().build().pinDao())

    @Test fun backupThenRestoreOnAFreshPhone() = runTest {
        val photo = File(PhotoStore.dir(context), "pin_1.jpg").apply { writeText("jpeg bytes") }
        val first = repo()
        first.upsert(Pin(name = "Car", latitude = 19.0, longitude = 72.8, accuracyMeters = 4f, photoPaths = listOf(photo.path), createdAt = 1L))
        first.upsert(Pin(name = "Tent", latitude = 18.0, longitude = 73.0, accuracyMeters = 5f, createdAt = 2L))
        val file = ByteArrayOutputStream().also { PinBackup(context, first).backup(it) }.toByteArray()

        val fresh = repo()
        val r = PinBackup(context, fresh).restore(ByteArrayInputStream(file))
        assertThat(r.added).isEqualTo(2)
        val car = fresh.all().first { it.name == "Car" }
        assertThat(File(car.photoPaths.single()).readText()).isEqualTo("jpeg bytes")

        // Restoring again adds nothing.
        assertThat(PinBackup(context, fresh).restore(ByteArrayInputStream(file)).alreadyHere).isEqualTo(2)
        assertThat(fresh.all()).hasSize(2)
    }

    @Test fun aBackupCannotWriteOutsideTheApp() = runTest {
        val good = ByteArrayOutputStream().also { PinBackup(context, repo()).backup(it) }.toByteArray()
        val evil = ByteArrayOutputStream()
        ZipOutputStream(evil).use { z ->
            ZipInputStream(ByteArrayInputStream(good)).use { zin -> generateSequence { zin.nextEntry }.forEach { e -> z.putNextEntry(ZipEntry(e.name)); z.write(zin.readBytes()); z.closeEntry() } }
            z.putNextEntry(ZipEntry("photos/../../../escaped.txt")); z.write("x".toByteArray()); z.closeEntry()
        }
        PinBackup(context, repo()).restore(ByteArrayInputStream(evil.toByteArray()))
        assertThat(File(context.cacheDir.parentFile, "escaped.txt").exists()).isFalse()
        assertThat(File(context.filesDir.parentFile?.parentFile, "escaped.txt").exists()).isFalse()
    }
}
