/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.EventAuditingInformationAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.translation.Key.FEATURE_TOGGLE_VALIDATION_ERROR_FEATURE_NOT_FOUND
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextSnapshotStore
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.asFeatureId
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureCreatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDeletedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDisabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureEnabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureWhitelistActivatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.SubjectAddedToWhitelistEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.SubjectDeletedFromWhitelistEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model.Feature
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.repository.FeatureRepository
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.shared.model.WhitelistedSubject
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.DISABLED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.ENABLED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.WHITELIST_ACTIVATED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
@Suppress("TooManyFunctions")
class FeatureSnapshotStore(private val repository: FeatureRepository) :
    AbstractSnapshotStoreJpa<SpecificRecordBase, FeatureSnapshot, Feature, FeatureId>(),
    FeaturetoggleContextSnapshotStore {
  override fun findInternal(identifier: UUID): Feature? =
      repository.findByIdentifier(identifier.asFeatureId())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      message is FeatureCreatedEventAvro ||
          message is FeatureEnabledEventAvro ||
          message is FeatureWhitelistActivatedEventAvro ||
          message is FeatureDisabledEventAvro ||
          message is FeatureDeletedEventAvro ||
          message is SubjectAddedToWhitelistEventAvro ||
          message is SubjectDeletedFromWhitelistEventAvro

  override fun isDeletedEvent(message: SpecificRecordBase): Boolean = false

  override fun updateInternal(event: SpecificRecordBase, currentSnapshot: Feature?): Long =
      when (event) {
        is FeatureCreatedEventAvro -> createSnapshot(currentSnapshot, event)
        is FeatureEnabledEventAvro ->
            updateState(mustExist(currentSnapshot), ENABLED, event.auditingInformation)
        is FeatureWhitelistActivatedEventAvro ->
            updateState(mustExist(currentSnapshot), WHITELIST_ACTIVATED, event.auditingInformation)
        is FeatureDisabledEventAvro ->
            updateState(mustExist(currentSnapshot), DISABLED, event.auditingInformation)
        is FeatureDeletedEventAvro -> deleteSnapshot(mustExist(currentSnapshot))
        is SubjectAddedToWhitelistEventAvro ->
            addSubjectToWhitelist(mustExist(currentSnapshot), event)
        is SubjectDeletedFromWhitelistEventAvro ->
            deleteSubjectFromWhitelist(mustExist(currentSnapshot), event)
        else -> error("Snapshot can't handle event ${event::class}. This should not happen.")
      }

  private fun createSnapshot(
      currentSnapshot: Feature?,
      event: FeatureCreatedEventAvro,
  ): Long {
    require(currentSnapshot == null) {
      throw DataIntegrityViolationException(VALIDATION_ERROR_SNAPSHOT_ALREADY_PRESENT)
    }
    val createdSnapshot =
        Feature().apply {
          identifier = FeatureId(event.aggregateIdentifier.identifier)
          name = event.featureName
          version = event.aggregateIdentifier.version
          setCreatedBy(UserId(event.auditingInformation.user.toUUID()))
          setCreatedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
          setLastModifiedBy(UserId(event.auditingInformation.user.toUUID()))
          setLastModifiedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
        }
    return repository.saveAndFlush(createdSnapshot).version
  }

  private fun updateState(
      currentSnapshot: Feature,
      state: FeatureStateEnum,
      eventAuditingInformationAvro: EventAuditingInformationAvro
  ): Long =
      repository
          .saveAndFlush(
              currentSnapshot.apply {
                this.state = state
                this.applyAuditingInformation(eventAuditingInformationAvro)
              })
          .version

  private fun deleteSnapshot(currentSnapshot: Feature): Long =
      repository.delete(currentSnapshot).let { currentSnapshot.version + 1 }

  private fun addSubjectToWhitelist(
      currentSnapshot: Feature,
      event: SubjectAddedToWhitelistEventAvro
  ): Long =
      repository
          .saveAndFlush(
              currentSnapshot.apply {
                whitelistedSubjects.add(
                    WhitelistedSubject(
                        event.subjectRef.toUUID(),
                        SubjectTypeEnum.valueOf(event.type),
                        event.featureName))
                this.applyAuditingInformation(event.auditingInformation)
              })
          .version

  private fun deleteSubjectFromWhitelist(
      currentSnapshot: Feature,
      event: SubjectDeletedFromWhitelistEventAvro
  ): Long =
      repository
          .saveAndFlush(
              currentSnapshot.apply {
                whitelistedSubjects.removeIf { it.subjectRef == event.subjectRef.toUUID() }
                this.applyAuditingInformation(event.auditingInformation)
              })
          .version

  private fun Feature.applyAuditingInformation(auditingInformation: EventAuditingInformationAvro) {
    this.setLastModifiedBy(auditingInformation.user.toUUID().asUserId())
    this.setLastModifiedDate(auditingInformation.date.toLocalDateTimeByMillis())
  }

  override fun findOrFail(identifier: FeatureId): FeatureSnapshot =
      throw NotImplementedError("not needed")

  fun findByFeatureNameOrFail(featureName: String): FeatureSnapshot =
      repository.findByName(featureName)?.asValueObject()
          ?: throw AggregateNotFoundException(
              FEATURE_TOGGLE_VALIDATION_ERROR_FEATURE_NOT_FOUND, featureName)

  fun exists(featureName: String): Boolean = repository.existsByName(featureName)

  private fun <FeatureSnapshot> mustExist(value: FeatureSnapshot?): FeatureSnapshot =
      requireNotNull(value) {
        throw DataIntegrityViolationException(VALIDATION_ERROR_SNAPSHOT_MISSING)
      }

  companion object {
    const val VALIDATION_ERROR_SNAPSHOT_ALREADY_PRESENT =
        "currentSnapshot cannot exist already on create"
    const val VALIDATION_ERROR_SNAPSHOT_MISSING =
        "currentSnapshot cannot be null on update / delete"
  }
}
