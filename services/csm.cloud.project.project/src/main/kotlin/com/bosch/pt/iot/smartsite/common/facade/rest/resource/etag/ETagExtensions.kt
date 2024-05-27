/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_ENTITY_OUTDATED
import com.bosch.pt.iot.smartsite.common.model.AbstractEntity
import com.bosch.pt.iot.smartsite.common.model.VersionedEntity
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(ETag::class.java)

fun ETag.Companion.from(value: Long): ETag = from(value.toString())

fun ETag.Companion.getVersionInformation(entity: VersionedEntity): String {
  requireNotNull(entity.version) { "Entity version must not be null!" }
  return entity.version.toString()
}

fun ETag.Companion.getVersionInformation(entity: AbstractSnapshotEntity<*, *>): String {
  requireNotNull(entity.version) { "Entity version must not be null!" }
  return entity.version.toString()
}

fun VersionedEntity.toEtagString(): String = ETag.getVersionInformation(this)

fun AbstractSnapshotEntity<*, *>.toEtagString(): String = ETag.getVersionInformation(this)

fun VersionedEntity.toEtag(): ETag = ETag.from(ETag.getVersionInformation(this))

fun AbstractSnapshotEntity<*, *>.toEtag(): ETag = ETag.from(ETag.getVersionInformation(this))

fun ETag.verify(entity: AbstractEntity<*, *>) {
  if (this != entity.toEtag()) {
    LOGGER.debug(
        "Provided ETag/version ({}) for entity ({}) with id ({}) is outdated.",
        entity.toEtagString(),
        entity.javaClass.simpleName,
        entity.id)

    throw EntityOutdatedException(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)
  }
}

fun ETag.verify(entity: AbstractSnapshotEntity<*, *>) {
  if (this != entity.toEtag()) {
    LOGGER.debug(
        "Provided ETag/version ({}) for entity ({}) with identifier ({}) is outdated.",
        entity.toEtagString(),
        entity.javaClass.simpleName,
        entity.identifier)

    throw EntityOutdatedException(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)
  }
}

fun ETag.verify(version: Long) {
  if (this != ETag.from(version)) {
    throw EntityOutdatedException(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)
  }
}
