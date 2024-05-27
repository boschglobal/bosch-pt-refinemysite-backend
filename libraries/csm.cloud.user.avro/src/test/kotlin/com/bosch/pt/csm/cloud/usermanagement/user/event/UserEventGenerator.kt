/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.event

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.DE
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAuditingInformation
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro.MALE
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberTypeEnumAvro.BUSINESS
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.CREATED
import java.time.Instant
import java.time.LocalDate.now
import java.util.Locale.UK

@JvmOverloads
fun EventStreamGenerator.submitUser(
    asReference: String,
    auditUserReference: String? = null,
    eventType: UserEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((UserAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingUser = get<UserAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((UserAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.auditingInformationBuilder.createdByBuilder = it.aggregateIdentifierBuilder
    it.auditingInformationBuilder.lastModifiedByBuilder = it.aggregateIdentifierBuilder

    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val userEvent =
      existingUser.buildEventAvro(eventType, defaultAggregateModifications, aggregateModifications)

  val sentEvent = send("user", asReference, null, userEvent, time.toEpochMilli()) as UserEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()
  getContext().lastRootContextIdentifier = sentEvent.getAggregate().getAggregateIdentifier()

  return this
}

@JvmOverloads
fun EventStreamGenerator.submitUserAndActivate(
    asReference: String,
    auditUserReference: String? = null,
    eventType: UserEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((UserAggregateAvro.Builder) -> Unit)? = null
) =
    this.apply {
      submitUser(asReference, auditUserReference, eventType, time, aggregateModifications)
      setUserContext(asReference)
    }

fun EventStreamGenerator.submitSystemUserAndActivate() =
    this.apply {
      submitUserAndActivate(asReference = "system") {
        it.userId = "SYSTEM"
        it.aggregateIdentifierBuilder.identifier = "c37da613-8e70-4003-9106-12412c9d2496"
        it.crafts = emptyList()
        it.email = "smartsiteapp+system@gmail.com"
        it.registered = true
        it.admin = false
        it.firstName = "Smartsite"
        it.lastName = "System"
      }
      setUserContext("system")
    }

fun EventStreamGenerator.submitTestAdminUserAndActivate() =
    this.apply {
      submitUserAndActivate(asReference = "admin") {
        it.userId = "6c306460-2e26-4dda-adb4-1ebc490c51ae"
        it.aggregateIdentifierBuilder.identifier = "eefc637a-ed01-4354-b737-f200eb13763b"
        it.crafts = emptyList()
        it.email = "smartsiteapp+testadmin@gmail.com"
        it.registered = true
        it.admin = true
        it.firstName = "Smartsite"
        it.lastName = "Admin"
      }
      setUserContext("admin")
    }

fun EventStreamGenerator.submitAnnouncementUserAndActivate() =
    this.apply {
      submitUserAndActivate(asReference = "announcement") {
        it.userId = "902bd482-f041-40de-8fdd-61ebc5a1d3c5"
        it.aggregateIdentifierBuilder.identifier = "35fa5881-d75f-ecd2-feac-72ee564542a9"
        it.crafts = emptyList()
        it.email = "smartsiteapp+announcement@gmail.com"
        it.registered = true
        it.admin = false
        it.firstName = "Smartsite"
        it.lastName = "Announcement"
      }
      setUserContext("announcement")
    }

@JvmOverloads
fun EventStreamGenerator.submitUserTombstones(
    reference: String = "user",
    messageKey: EventMessageKey? = null
): EventStreamGenerator {
  val user = get<UserAggregateAvro>(reference)!!

  if (messageKey == null) {
    val maxVersion = user.getAggregateIdentifier().getVersion()
    val userIdentifier = user.getAggregateIdentifier()
    for (version in 0..maxVersion) {
      val key =
          AggregateEventMessageKey(
              AggregateIdentifier(
                  userIdentifier.getType(), userIdentifier.getIdentifier().toUUID(), version),
              userIdentifier.getIdentifier().toUUID())
      sendTombstoneMessage("user", reference, key)
    }
  } else {
    sendTombstoneMessage("user", reference, messageKey)
  }
  return this
}

private fun UserAggregateAvro?.buildEventAvro(
    eventType: UserEventEnumAvro,
    vararg blocks: ((UserAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { UserEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newUser(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newUser(event: UserEventEnumAvro = CREATED): UserEventAvro.Builder {
  val user =
      UserAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(newAggregateIdentifier(USER.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setUserId(randomString())
          .setEmail(randomString() + "@test.de")
          .setFirstName(randomString())
          .setGender(MALE)
          .setLastName(randomString())
          .setPhoneNumbers(
              listOf(
                  PhoneNumberAvro.newBuilder()
                      .setPhoneNumberType(BUSINESS)
                      .setCountryCode("0049")
                      .setCallNumber("123456")
                      .build()))
          .setRegistered(true)
          .setAdmin(false)
          .setCrafts(emptyList())
          .setEulaAcceptedDate(now().toEpochMilli())
          .setCountry(DE)
          .setLocale(UK.toString())

  return UserEventAvro.newBuilder().setAggregateBuilder(user).setName(event)
}
