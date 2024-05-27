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
import com.bosch.pt.csm.cloud.featuretoggle.common.command.emitSingleEvent
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextLocalEventBus
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.DisableFeatureCommand
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureDisabledEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshot
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshotStore
import java.time.LocalDateTime.now
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DisableFeatureCommandHandler(
    val snapshotStore: FeatureSnapshotStore,
    val eventBus: FeaturetoggleContextLocalEventBus
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: DisableFeatureCommand) =
      CommandHandler.of(snapshotStore.findByFeatureNameOrFail(command.featureName))
          .emitSingleEvent { it.toEvent() }
          .to(eventBus)
          .andReturnSnapshot()
          .identifier

  private fun FeatureSnapshot.toEvent() =
      FeatureDisabledEvent(this.identifier, this.name, getCurrentUserReference().identifier, now())
}
