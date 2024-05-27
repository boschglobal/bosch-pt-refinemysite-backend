/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler

import com.bosch.pt.csm.cloud.usermanagement.common.query.QueryUtilities.assertSize
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_ASSOCIATED_CRAFT_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.query.CraftQueryService
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UpdateUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.PhoneNumberValueObject
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.UserSnapshotStore
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateUserCommandHandler(
    private val eventBus: UserContextLocalEventBus,
    private val craftQueryService: CraftQueryService,
    private val snapshotStore: UserSnapshotStore
) {

  @PreAuthorize("@userAuthorizationComponent.isCurrentUser(#command.identifier)")
  @Transactional
  fun handle(command: UpdateUserCommand) =
      snapshotStore
          .findOrFail(command.identifier)
          .toCommandHandler()
          .assertVersionMatches(command.version)
          .applyChanges { user ->
            user.gender = command.gender
            user.firstName = command.firstName
            user.lastName = command.lastName
            user.position = command.position
            user.crafts =
                findCraftsOrFail(command.crafts).map { it.asAggregateIdentifier() }.toSet()
            user.phonenumbers = command.phoneNumbers.map { PhoneNumberValueObject(it) }.toSet()
            user.locale = command.locale
            user.country = command.country
          }
          .emitEvent(UPDATED)
          .ifSnapshotWasChanged()
          .to(eventBus)
          .let {}

  private fun findCraftsOrFail(craftIds: Collection<CraftId>): Set<Craft> =
      craftQueryService
          .findByIdentifierIn(craftIds)
          .assertSize(craftIds.size, USER_VALIDATION_ERROR_ASSOCIATED_CRAFT_NOT_FOUND)
}
