/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application

import org.bson.types.ObjectId
import org.springframework.core.convert.converter.Converter
import java.util.UUID

class BsonToUuidConverter : Converter<ObjectId, UUID> {
    override fun convert(source: ObjectId): UUID? = UUID.nameUUIDFromBytes(source.toByteArray())
}
