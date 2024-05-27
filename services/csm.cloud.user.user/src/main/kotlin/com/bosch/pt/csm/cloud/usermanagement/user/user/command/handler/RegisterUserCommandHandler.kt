/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.US
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper.executeAuthenticatedAs
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper.setAuthenticationContext
import com.bosch.pt.csm.cloud.usermanagement.common.query.QueryUtilities.assertSize
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_ASSOCIATED_CRAFT_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_EULA_NOT_ACCEPTED_REGISTRATION
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_REGISTRATION_DATA_INVALID
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.query.CraftQueryService
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.RegisterUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.PhoneNumberValueObject
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore.UserSnapshot
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.UserRepository
import java.time.LocalDate
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

@Service
class RegisterUserCommandHandler(
    private val eventBus: UserContextLocalEventBus,
    private val idGenerator: IdGenerator,
    private val craftQueryService: CraftQueryService,
    private val userRepository: UserRepository,
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UserId
) {

  @PreAuthorize("@userAuthorizationComponent.isCurrentUser(#command.externalUserId)")
  @Transactional
  fun handle(command: RegisterUserCommand): UserId =
      executeAuthenticatedAs(userRepository.findOneByIdentifier(systemUserIdentifier)!!) {
            UserSnapshot(
                    identifier = UserId(idGenerator.generateId()),
                    version = INITIAL_SNAPSHOT_VERSION,
                    createdDate = null,
                    createdBy = null,
                    lastModifiedDate = null,
                    lastModifiedBy = null,
                    externalUserId = command.externalUserId,
                    gender = command.gender,
                    firstName = command.firstName,
                    lastName = command.lastName,
                    email = command.email,
                    position = command.position,
                    admin = false,
                    registered = true,
                    locked = false,
                    eulaAcceptedDate = if (command.eulaAccepted) LocalDate.now() else null,
                    locale = command.locale,
                    country = command.country,
                    crafts = findCraftsOrFail(command).map { it.asAggregateIdentifier() }.toSet(),
                    phonenumbers = command.phoneNumbers.map { PhoneNumberValueObject(it) }.toSet())
                .toCommandHandler()
                .checkPrecondition { command.eulaAccepted || command.country != US }
                .onFailureThrow(USER_VALIDATION_ERROR_EULA_NOT_ACCEPTED_REGISTRATION)
                .checkPrecondition { isUserDataValid(command) }
                .onFailureThrow(USER_VALIDATION_ERROR_REGISTRATION_DATA_INVALID)
                .emitEvent(CREATED)
                .to(eventBus)
                .andReturnSnapshot()
                .identifier
          }
          .apply { setAuthenticationContext(userRepository.findOneByIdentifier(this)!!) }

  private fun isUserDataValid(command: RegisterUserCommand) =
      command.firstName.isNotBlank() &&
          command.lastName.isNotBlank() &&
          command.externalUserId.isNotBlank() &&
          command.email.isNotBlank() &&
          (command.position == null || command.position.isNotBlank())

  private fun findCraftsOrFail(command: RegisterUserCommand): Set<Craft> =
      craftQueryService
          .findByIdentifierIn(command.crafts)
          .assertSize(command.crafts.size, USER_VALIDATION_ERROR_ASSOCIATED_CRAFT_NOT_FOUND)
}
