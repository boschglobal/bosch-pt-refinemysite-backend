/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.command.handler

import com.bosch.pt.csm.cloud.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditUserExtractor.getCurrentUserReference
import com.bosch.pt.csm.cloud.common.translation.Key.FEATURE_TOGGLE_VALIDATION_ERROR_FEATURE_ALREADY_EXISTS
import com.bosch.pt.csm.cloud.featuretoggle.common.command.emitSingleEvent
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextLocalEventBus
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.CreateFeatureCommand
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureCreatedEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.asFeatureId
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshot
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshotStore
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateFeatureCommandHandler(
    val snapshotStore: FeatureSnapshotStore,
    val eventBus: FeaturetoggleContextLocalEventBus
) {
  @AdminAuthorization
  @Transactional
  fun handle(command: CreateFeatureCommand) =
      CommandHandler.of(FeatureSnapshot(identifier = randomUUID().asFeatureId(), name = command.featureName))
          .checkPrecondition { !snapshotStore.exists(it.name) }
          .onFailureThrow(FEATURE_TOGGLE_VALIDATION_ERROR_FEATURE_ALREADY_EXISTS)
          .emitSingleEvent { it.toEvent() }
          .to(eventBus)
          .andReturnSnapshot()
          .identifier

  private fun FeatureSnapshot.toEvent() =
      FeatureCreatedEvent(this.identifier, this.name, getCurrentUserReference().identifier, now())
}
