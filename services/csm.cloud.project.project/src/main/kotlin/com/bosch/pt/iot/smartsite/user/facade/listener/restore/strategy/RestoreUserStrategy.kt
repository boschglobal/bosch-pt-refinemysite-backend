/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.REGISTERED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.craft.repository.CraftRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.user.model.GenderEnum
import com.bosch.pt.iot.smartsite.user.model.PhoneNumber
import com.bosch.pt.iot.smartsite.user.model.PhoneNumberType
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.repository.ProfilePictureRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.LocaleUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreUserStrategy(
    private val craftRepository: CraftRepository,
    private val profilePictureRepository: ProfilePictureRepository,
    private val participantRepository: ParticipantRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, userRepository),
    UserContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      USER.value == record.key().aggregateIdentifier.type && record.value() is UserEventAvro?

  override fun doHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    val event = record.value() as UserEventAvro?

    if (event == null) {
      deleteUser(record.key())
    } else if (event.getName() == CREATED ||
        event.getName() == REGISTERED ||
        event.getName() == UPDATED) {

      val aggregate = event.getAggregate()
      val user = findUser(aggregate.getAggregateIdentifier())

      if (user == null) {
        createUser(event)
      } else {
        updateUser(user, aggregate)
      }

      setEMailOnParticipants(
          aggregate.getAggregateIdentifier().getIdentifier().toUUID(), aggregate.getEmail())
    } else {
      handleInvalidEventType(event.getName().name)
    }
  }

  private fun createUser(userEventAvro: UserEventAvro) {
    val aggregate = userEventAvro.getAggregate()
    val user =
        User().apply {
          setBasicUserAttributes(this, aggregate)
          setCrafts(this, aggregate)
          setPhoneNumbers(this, aggregate)
        }

    val auditingInformation = aggregate.getAuditingInformation()
    if (isSelfReferencing(user, auditingInformation)) {
      // Save the user without self-reference
      entityManager.persist(user)

      // Since hibernate 6 it seems to be required to flush between inserting and updating
      entityManager.flush()

      // It's require to detach the entity to set versions manually
      update(
          user,
          object : DetachedEntityUpdateCallback<User> {
            override fun update(entity: User) {
              setAuditAttributes(entity, auditingInformation)
            }
          })
    } else {

      setAuditAttributes(user, auditingInformation)
      entityManager.persist(user)
    }
  }

  private fun updateUser(user: User, aggregate: UserAggregateAvro) =
      update(
          user,
          object : DetachedEntityUpdateCallback<User> {
            override fun update(entity: User) {
              setBasicUserAttributes(entity, aggregate)
              setCrafts(entity, aggregate)
              setPhoneNumbers(entity, aggregate)
              setAuditAttributes(entity, aggregate.auditingInformation)
            }
          })

  private fun deleteUser(key: AggregateEventMessageKey) {
    val identifier = key.aggregateIdentifier.identifier
    val user = userRepository.findWithDetailsByIdentifier(identifier)
    if (user != null) {
      delete(profilePictureRepository.findOneByUserIdentifier(identifier))
      update(
          user,
          object : DetachedEntityUpdateCallback<User> {
            override fun update(entity: User) {
              entity.version = key.aggregateIdentifier.version
              entity.anonymize()
            }
          })
    }
  }

  private fun setBasicUserAttributes(user: User, aggregate: UserAggregateAvro) {
    user.identifier = aggregate.getAggregateIdentifier().getIdentifier().toUUID()
    user.version = aggregate.getAggregateIdentifier().getVersion()
    user.admin = aggregate.getAdmin()
    user.email = aggregate.getEmail()
    user.firstName = aggregate.getFirstName()
    user.lastName = aggregate.getLastName()
    user.position = aggregate.getPosition()
    user.gender =
        if (aggregate.getGender() == null) null else GenderEnum.valueOf(aggregate.getGender().name)
    user.registered = aggregate.getRegistered()
    user.ciamUserIdentifier = aggregate.getUserId()
    user.deleted = false
    user.locked = aggregate.getLocked()
    user.setUserLocale(
        if (aggregate.getLocale() == null) null else LocaleUtils.toLocale(aggregate.getLocale()))
    user.country =
        if (aggregate.getCountry() == null) null
        else IsoCountryCodeEnum.valueOf(aggregate.getCountry().name)
  }

  private fun setPhoneNumbers(user: User, aggregate: UserAggregateAvro) {
    val phoneNumbers =
        aggregate.getPhoneNumbers().map { phoneNumberAvro: PhoneNumberAvro ->
          PhoneNumber(
              PhoneNumberType.valueOf(phoneNumberAvro.getPhoneNumberType().name),
              phoneNumberAvro.getCountryCode(),
              phoneNumberAvro.getCallNumber())
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
    val craftIds =
        aggregate.getCrafts().map { craftIdentifier: AggregateIdentifierAvro ->
          craftIdentifier.getIdentifier().toUUID()
        }

    val crafts = craftRepository.findByIdentifierIn(craftIds)
    require(craftIds.size == crafts.size) { "Crafts missing" }

    user.crafts.clear()
    user.crafts.addAll(crafts)
  }

  private fun isSelfReferencing(user: User, auditingInformation: AuditingInformationAvro): Boolean =
      isUser(user, auditingInformation.getCreatedBy()) ||
          isUser(user, auditingInformation.getLastModifiedBy())

  private fun isUser(user: User?, identifierToCheck: AggregateIdentifierAvro): Boolean =
      user != null && user.identifier == identifierToCheck.getIdentifier().toUUID()

  private fun findUser(aggregateIdentifierAvro: AggregateIdentifierAvro): User? =
      userRepository.findWithDetailsByIdentifier(aggregateIdentifierAvro.getIdentifier().toUUID())

  private fun setEMailOnParticipants(userIdentifier: UUID, email: String) =
      participantRepository.findAllByUserIdentifier(userIdentifier).forEach {
        update(
            it,
            object : DetachedEntityUpdateCallback<Participant> {
              override fun update(entity: Participant) {
                entity.email = email
              }
            })
      }
}
