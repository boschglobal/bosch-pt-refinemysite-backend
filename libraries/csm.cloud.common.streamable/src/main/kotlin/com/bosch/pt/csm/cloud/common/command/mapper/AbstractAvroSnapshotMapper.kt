/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.UserReference
import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditUserExtractor
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import java.time.LocalDateTime
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase

abstract class AbstractAvroSnapshotMapper<T> : AvroSnapshotMapper<T> where
T : VersionedSnapshot,
T : AuditableSnapshot {

  /**
   * This is the generic implementation of the corresponding interface method. It does not have to
   * be overwritten by an implementation of this class.
   */
  override fun toMessageKeyWithCurrentVersion(snapshot: T) =
      AggregateEventMessageKey(
          toAggregateIdentifierAvro(snapshot.identifier, snapshot.version)
              .buildAggregateIdentifier(),
          getRootContextIdentifier(snapshot))

  /**
   * This is the generic implementation of the corresponding interface method. It does not have to
   * be overwritten by an implementation of this class.
   */
  override fun toMessageKeyWithNewVersion(snapshot: T) =
      AggregateEventMessageKey(
          toAggregateIdentifierAvroWithNextVersion(snapshot).buildAggregateIdentifier(),
          getRootContextIdentifier(snapshot))

  /**
   * This is the main method to be implemented. It generates the avro type representing an event of
   * the aggregate containing the entire snapshot of the aggregate. Use the two methods
   * [toAggregateIdentifierAvroWithNextVersion] and [toUpdatedAuditingInformationAvro] provided by
   * this class to implement this method
   */
  abstract override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: T,
      eventType: E
  ): SpecificRecordBase

  /**
   * Use this method in the implementation of toAvroMessageWithNewVersion() to get the aggregate
   * identifier with the next applicable version
   */
  protected fun toAggregateIdentifierAvroWithNextVersion(snapshot: T) =
      toAggregateIdentifierAvro(snapshot.identifier, snapshot.version.inc())

  /**
   * Generates the current auditing information required for an aggregate AVRO representation. It
   * initializes the created by information if required (when this is called in the context of an
   * aggregate creation) and always updates the last modified by user information.
   */
  protected fun toUpdatedAuditingInformationAvro(snapshot: T): AuditingInformationAvro =
      LocalDateTime.now().let {
        AuditingInformationAvro.newBuilder()
            .setCreatedBy(createdByUserReference(snapshot).toAggregateIdentifier())
            .setCreatedDate((snapshot.createdDate ?: it).toEpochMilli())
            .setLastModifiedBy(lastModifiedByUserReference().toAggregateIdentifier())
            .setLastModifiedDate(it.toEpochMilli())
            .build()
      }

  /**
   * Generates an instance of AggregateIdentifierAvro by getting the aggregate type from the
   * implementation of the abstract function getAggregateType()
   */
  private fun toAggregateIdentifierAvro(
      identifier: UuidIdentifiable,
      version: Long
  ): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(identifier.toString())
          .setVersion(version)
          .setType(getAggregateType())
          .build()

  /**
   * Overwrite this method to determine the aggregate type, e.g. with =
   * ProjectmanagementAggregateTypeEnum.INVITATION.value
   */
  protected abstract fun getAggregateType(): String

  /** Overwrite this method to determine the root context identifier */
  protected abstract fun getRootContextIdentifier(snapshot: T): UUID

  /**
   * When the createdBy field is empty, the user reference is taken from the security context using
   * [AuditUserExtractor]. Otherwise it is constructed using the the createdBy [UserId] and the fake
   * version 0. This fake version is a workaround since we only store the user id of the created by
   * user, not the version. It was recognized that the version of the user for the created by
   * reference is (if at all) only relevant when an aggregate instance is created.
   */
  private fun createdByUserReference(snapshot: T) =
      snapshot.createdBy?.let { UserReference(it, 0) }
          ?: AuditUserExtractor.getCurrentUserReference()

  /**
   * Return a reference to the current user of the security context by using [AuditUserExtractor]
   * since this is the user that is currently modifying the aggregate and therefore to be referenced
   * here.
   */
  private fun lastModifiedByUserReference() = AuditUserExtractor.getCurrentUserReference()
}
