/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.repository.CraftRepository
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.REGISTERED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.repository.ProfilePictureRepository
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumber
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumberType
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.UserRepository
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.LocaleUtils
import org.springframework.stereotype.Component
import org.springframework.util.Assert

@Component
class UserSnapshotStore(
    private val craftRepository: CraftRepository,
    private val profilePictureRepository: ProfilePictureRepository,
    private val repository: UserRepository,
) :
    AbstractSnapshotStoreJpa<UserEventAvro, UserSnapshot, User, UserId>(),
    UserContextSnapshotStore {

  override fun findOrFail(identifier: UserId): UserSnapshot =
      repository.findWithDetailsByIdentifier(identifier)?.asValueObject()
          ?: throw AggregateNotFoundException(
              USER_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      key.aggregateIdentifier.type == USER.name &&
          message is UserEventAvro &&
          message.getName().let { it == CREATED || it == REGISTERED || it == UPDATED }

  override fun handlesTombstoneMessage(key: AggregateEventMessageKey): Boolean =
      key.aggregateIdentifier.type == USER.name

  override fun updateInternal(event: UserEventAvro, currentSnapshot: User?) =
      when (currentSnapshot == null) {
        true -> createUser(event)
        else -> updateUser(currentSnapshot, event)
      }

  override fun handleTombstoneMessage(key: AggregateEventMessageKey) {
    UserId(key.aggregateIdentifier.identifier).apply {
      profilePictureRepository.deleteByUserIdentifier(this)
      repository.deleteByIdentifier(this)
    }
  }

  override fun findInternal(identifier: UUID): User? =
      repository.findOneByIdentifier(UserId(identifier))

  private fun createUser(event: UserEventAvro) = updateUser(User(), event)

  private fun updateUser(user: User, event: UserEventAvro): Long {
    val aggregate = event.getAggregate()
    return user
        .apply {
          setAuditAttributes(this, aggregate.getAuditingInformation())
          setBasicUserAttributes(this, aggregate)
          setCrafts(this, aggregate)
          setPhoneNumbers(this, aggregate)
        }
        .let { repository.saveAndFlush(it).version }
  }

  private fun setBasicUserAttributes(user: User, aggregate: UserAggregateAvro) {
    user.apply {
      identifier = UserId(aggregate.getIdentifier())
      admin = aggregate.getAdmin()
      email = aggregate.getEmail()
      eulaAcceptedDate =
          if (aggregate.getEulaAcceptedDate() == null) null
          else aggregate.getEulaAcceptedDate().toLocalDateByMillis()
      firstName = aggregate.getFirstName()
      lastName = aggregate.getLastName()
      position = aggregate.getPosition()
      gender =
          if (aggregate.getGender() == null) null
          else GenderEnum.valueOf(aggregate.getGender().name)
      registered = aggregate.getRegistered()
      externalUserId = aggregate.getUserId()
      locked = aggregate.getLocked()
      locale =
          if (aggregate.getLocale() == null) null else LocaleUtils.toLocale(aggregate.getLocale())
      country =
          if (aggregate.getCountry() == null) null
          else IsoCountryCodeEnum.valueOf(aggregate.getCountry().name)
    }
  }

  private fun setPhoneNumbers(user: User, aggregate: UserAggregateAvro) {
    val phoneNumbers: List<PhoneNumber> =
        aggregate.getPhoneNumbers().map {
          PhoneNumber(
              PhoneNumberType.valueOf(it.getPhoneNumberType().name),
              it.getCountryCode(),
              it.getCallNumber())
        }

    val userPhoneNumbers = user.phonenumbers
    val numbersToAdd = CollectionUtils.subtract(phoneNumbers, userPhoneNumbers)
    val numbersToRemove = CollectionUtils.subtract(userPhoneNumbers, phoneNumbers)
    if (!numbersToAdd.isEmpty()) {
      userPhoneNumbers.addAll(numbersToAdd)
    }
    if (!numbersToRemove.isEmpty()) {
      userPhoneNumbers.removeAll(numbersToRemove)
    }
  }

  private fun setCrafts(user: User, aggregate: UserAggregateAvro) {
    val craftIds = aggregate.getCrafts().map { CraftId(it.getIdentifier().toUUID()) }
    val crafts = craftRepository.findByIdentifierIn(craftIds)
    Assert.isTrue(craftIds.size == crafts.size, "Crafts missing")

    val userCrafts = user.crafts
    val craftsToAdd = CollectionUtils.subtract(crafts, userCrafts)
    val craftsToRemove = CollectionUtils.subtract(userCrafts, crafts)

    if (!craftsToAdd.isEmpty()) {
      userCrafts.addAll(craftsToAdd)
    }
    if (!craftsToRemove.isEmpty()) {
      userCrafts.removeAll(craftsToRemove)
    }
  }

  override fun isDeletedEvent(message: SpecificRecordBase) = false
}
