package com.aichat.core.domain.service

import com.aichat.core.domain.model.FactsData

interface FactsStorage {
    suspend fun getFacts(chatId: String): FactsData?
    suspend fun upsertFacts(facts: FactsData)
}