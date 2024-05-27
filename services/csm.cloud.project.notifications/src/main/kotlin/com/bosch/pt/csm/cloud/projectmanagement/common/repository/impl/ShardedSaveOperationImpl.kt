/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Sharded
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.Update

class ShardedSaveOperationImpl<T : Sharded, I>
constructor(private val mongoOperations: MongoOperations) : ShardedSaveOperation<T, I> {

    private val operations: EntityOperations = EntityOperations(mongoOperations.converter.mappingContext)

    override fun <S : T> save(entity: S): S {
        val mappedDocument = operations.forEntity(entity).toMappedDocument(mongoOperations.converter)
        val query = query(
            where("_id")
                .`is`(mappedDocument.id)
                .and(entity.shardKeyName)
                .`is`(entity.shardKeyValue)
        )

        val update = Update.fromDocument(mappedDocument.document)
        mongoOperations.upsert(query, update, entity.javaClass)
        return entity
    }
}
