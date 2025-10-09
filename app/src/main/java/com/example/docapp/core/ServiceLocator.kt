package com.example.docapp.core

import android.content.Context
import com.example.docapp.data.AppDb
import com.example.docapp.data.RepositoriesImpl
import com.example.docapp.data.SqlDaoFactory
import com.example.docapp.domain.Repositories
import com.example.docapp.domain.usecases.UseCases

object ServiceLocator {
    lateinit var db: AppDb
        private set
    lateinit var crypto: CryptoManager
        private set
    lateinit var files: AttachmentStore
        private set
    lateinit var repos: Repositories
        private set
    lateinit var useCases: UseCases
        private set

    fun init(appCtx: Context) {
        db = AppDb(appCtx)
        val daos = SqlDaoFactory(db)
        crypto = CryptoManagerImpl(appCtx)
        files = AttachmentStoreImpl(appCtx)
        repos = RepositoriesImpl(daos, crypto, files, appCtx)
        useCases = UseCases(repos)
    }
}
