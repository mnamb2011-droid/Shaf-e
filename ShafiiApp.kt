package com.shafii.encyclopedia

import android.app.Application
import com.shafii.encyclopedia.data.db.EncyclopediaSqliteStore
import com.shafii.encyclopedia.data.db.UserDataDatabase
import com.shafii.encyclopedia.data.repository.EncyclopediaRepository
import com.shafii.encyclopedia.util.DatabasePackManager

class ShafiiApp : Application() {

    lateinit var repository: EncyclopediaRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val packManager = DatabasePackManager(this)
        val sqliteStore = EncyclopediaSqliteStore(packManager)
        val userDb = UserDataDatabase.getInstance(this)
        repository = EncyclopediaRepository(
            store = sqliteStore,
            userDao = userDb.userDataDao(),
            packManager = packManager
        )
    }
}
