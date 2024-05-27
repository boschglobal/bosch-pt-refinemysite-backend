/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.api.CreateCraftCommand
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.snapshotstore.CraftSnapshot
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.snapshotstore.asSnapshot
import com.bosch.pt.csm.cloud.usermanagement.craft.eventstore.CraftContextLocalEventBus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateCraftCommandHandler(private val eventBus: CraftContextLocalEventBus) {

  @AdminAuthorization
  @Transactional
  fun handle(command: CreateCraftCommand) =
      CraftSnapshot(
              identifier = command.identifier,
              version = INITIAL_SNAPSHOT_VERSION,
              createdDate = null,
              createdBy = null,
              lastModifiedDate = null,
              lastModifiedBy = null,
              defaultName = command.defaultName,
              translations = command.translations.map { it.asSnapshot() })
          .toCommandHandler()
          .emitEvent(CREATED)
          .to(eventBus)
          .andReturnSnapshot()
          .identifier
}
