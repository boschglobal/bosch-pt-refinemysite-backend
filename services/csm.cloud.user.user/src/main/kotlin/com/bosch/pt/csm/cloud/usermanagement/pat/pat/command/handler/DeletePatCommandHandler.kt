/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler

import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.PAT_VALIDATION_ERROR_USER_MISMATCH
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.DeletePatCommand
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.mapper.PatTombstoneAvroSnapshotMapper
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.snapshotstore.PatSnapshotStore
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeletePatCommandHandler(
    private val snapshotStore: PatSnapshotStore,
    private val eventBus: PatLocalEventBus,
    private val patTombstoneAvroSnapshotMapper: PatTombstoneAvroSnapshotMapper
) {

  @Suppress("SpringElInspection")
  @PreAuthorize("#command.impersonatedUser == principal.identifier")
  @Transactional
  fun handle(command: DeletePatCommand) =
      CommandHandler.of(
              snapshotStore.findOrFail(command.patId),
              patTombstoneAvroSnapshotMapper,
          )
          .assertVersionMatches(command.version)
          .checkAuthorization { command.impersonatedUser == it.impersonatedUser }
          .onFailureThrow(PAT_VALIDATION_ERROR_USER_MISMATCH)
          .emitTombstone()
          .to(eventBus)
          .andReturnSnapshot()
          .identifier
}
