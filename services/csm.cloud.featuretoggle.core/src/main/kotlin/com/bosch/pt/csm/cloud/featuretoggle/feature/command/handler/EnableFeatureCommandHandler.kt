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
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.EnableFeatureCommand
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureEnabledEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshot
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore.FeatureSnapshotStore
import java.time.LocalDateTime.now
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EnableFeatureCommandHandler(
    val snapshotStore: FeatureSnapshotStore,
    val eventBus: FeaturetoggleContextLocalEventBus
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: EnableFeatureCommand) =
      CommandHandler.of(snapshotStore
        .findByFeatureNameOrFail(command.featureName))
          .emitSingleEvent { it.toEvent() }
          .to(eventBus)
          .andReturnSnapshot()
          .identifier

  private fun FeatureSnapshot.toEvent() =
      FeatureEnabledEvent(this.identifier, this.name, getCurrentUserReference().identifier, now())
}
