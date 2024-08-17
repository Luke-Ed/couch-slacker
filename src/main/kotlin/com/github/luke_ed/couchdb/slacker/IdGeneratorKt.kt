package com.github.luke_ed.couchdb.slacker

interface IdGeneratorKt<EntityT> {
    fun generateId(entity: EntityT): String

    fun getEntityClass(): Class<EntityT>
}