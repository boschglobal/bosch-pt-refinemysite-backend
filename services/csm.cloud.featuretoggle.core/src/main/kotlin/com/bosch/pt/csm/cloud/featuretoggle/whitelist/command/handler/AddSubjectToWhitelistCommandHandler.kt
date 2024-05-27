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
import com.bosch.pt.csm.cloud.common.translation.Key.FEATURE_TOGGLE_VALIDATION_ERROR_SUBJECT_WHITELISTED_ALREADY
import com.bosch.pt.csm.cloud.featuretoggle.common.command.emitSingleEvent
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextLocalEventBus
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshot
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshotStore
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api.AddSubjectToWhitelistCommand
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api.SubjectAddedToWhitelistEvent
import java.time.LocalDateTime
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddSubjectToWhitelistCommandHandler(
    val featureSnapshotStore: FeatureSnapshotStore,
    val eventBus: FeaturetoggleContextLocalEventBus
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: AddSubjectToWhitelistCommand): FeatureId =
      CommandHandler.of(featureSnapshotStore.findByFeatureNameOrFail(command.featureName))
          .checkPrecondition {
            it.whitelistedSubjects.none { one -> one.subjectRef == command.subjectRef }
          }
          .onFailureThrow(FEATURE_TOGGLE_VALIDATION_ERROR_SUBJECT_WHITELISTED_ALREADY)
          .emitSingleEvent { it.toEventFrom(command) }
          .to(eventBus)
          .andReturnSnapshot()
          .identifier

  private fun FeatureSnapshot.toEventFrom(command: AddSubjectToWhitelistCommand) =
      SubjectAddedToWhitelistEvent(
          this.identifier,
          command.subjectRef,
          command.type,
          command.featureName,
          getCurrentUserReference().identifier,
          LocalDateTime.now())
}
