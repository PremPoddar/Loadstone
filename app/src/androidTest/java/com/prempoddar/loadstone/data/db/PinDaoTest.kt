package com.prempoddar.loadstone.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class PinDaoTest {
    private lateinit var db: LoadstoneDatabase
    private lateinit var dao: PinDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LoadstoneDatabase::class.java,
        ).build()
        dao = db.pinDao()
    }

    @After fun teardown() = db.close()

    @Test fun insertAndObserve() = runBlocking {
        val id = dao.upsert(PinEntity(0, "Campsite", null, 37.0, -122.0, 4f, "TENT", 1000L))
        val all = dao.observeAll().first()
        assertThat(all).hasSize(1)
        assertThat(all.first().name).isEqualTo("Campsite")
        assertThat(id).isGreaterThan(0L)
    }

    @Test fun pinnedPinsComeFirstHoweverOldTheyAre() = runBlocking {
        val old = dao.upsert(PinEntity(0, "Old", null, 1.0, 2.0, 3f, "PIN", 1L))
        dao.upsert(PinEntity(0, "New", null, 1.0, 2.0, 3f, "PIN", 9_000L))
        dao.setPinned(old, true)
        assertThat(dao.observeAll().first().map { it.name }).containsExactly("Old", "New").inOrder()
    }

    @Test fun unpinningPutsItBackInOrder() = runBlocking {
        val old = dao.upsert(PinEntity(0, "Old", null, 1.0, 2.0, 3f, "PIN", 1L))
        dao.upsert(PinEntity(0, "New", null, 1.0, 2.0, 3f, "PIN", 9_000L))
        dao.setPinned(old, true)
        dao.setPinned(old, false)
        assertThat(dao.observeAll().first().map { it.name }).containsExactly("New", "Old").inOrder()
    }

    @Test fun deleteRemoves() = runBlocking {
        val id = dao.upsert(PinEntity(0, "X", null, 1.0, 2.0, 3f, "PIN", 1L))
        dao.deleteById(id)
        assertThat(dao.observeAll().first()).isEmpty()
    }
}
