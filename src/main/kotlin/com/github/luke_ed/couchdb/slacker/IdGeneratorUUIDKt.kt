package com.github.luke_ed.couchdb.slacker

import java.util.UUID

class IdGeneratorUUIDKt: IdGeneratorKt<Any> {
    override fun getEntityClass(): Class<Any> {
        return Any::class.java
    }

    override fun generateId(entity: Any): String {
        return UUID.randomUUID().toString()
    }

    inline fun <reified EntityT> generateId(entityT: EntityT): String {
        return UUID.randomUUID().toString()
    }
}