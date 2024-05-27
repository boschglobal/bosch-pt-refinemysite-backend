/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.handler

import com.bosch.pt.csm.cloud.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditUserExtractor.getCurrentUserReference
import com.bosch.pt.csm.cloud.common.translation.Key.FEATURE_TOGGLE_VALIDATION_ERROR_SUBJECT_NOT_FOUND_IN_WHITELIST
import com.bosch.pt.csm.cloud.featuretoggle.common.command.emitSingleEvent
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextLocalEventBus
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshot
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshotStore
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api.DeleteSubjectFromWhitelistCommand
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api.SubjectDeletedFromWhitelistEvent
import java.time.LocalDateTime
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteSubjectFromWhitelistCommandHandler(
    val snapshotStore: FeatureSnapshotStore,
    val eventBus: FeaturetoggleContextLocalEventBus
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: DeleteSubjectFromWhitelistCommand): FeatureId =
      CommandHandler.of(snapshotStore.findByFeatureNameOrFail(command.featureName))
          .checkPrecondition {
            it.whitelistedSubjects.any { one -> one.subjectRef == command.subjectRef }
          }
          .onFailureThrow(FEATURE_TOGGLE_VALIDATION_ERROR_SUBJECT_NOT_FOUND_IN_WHITELIST)
          .emitSingleEvent { it.toEventFrom(command) }
          .to(eventBus)
          .andReturnSnapshot()
          .identifier

  private fun FeatureSnapshot.toEventFrom(command: DeleteSubjectFromWhitelistCommand) =
      SubjectDeletedFromWhitelistEvent(
          this.identifier,
          command.subjectRef,
          this.whitelistedSubjects.first { it.subjectRef == command.subjectRef }.type,
          this.name,
          getCurrentUserReference().identifier,
          LocalDateTime.now())
}
