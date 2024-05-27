/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.event

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AggregateReferenceAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.pat.common.PatAggregateTypeEnum.PAT
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro.GRAPHQL_API_READ
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro.TIMELINE_API_READ
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatTypeEnumAvro.RMSPAT1
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatUpdatedEventAvro
import java.time.Instant
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.security.crypto.bcrypt.BCrypt

@JvmOverloads
@Suppress("unused")
fun EventStreamGenerator.submitPatCreated(
    asReference: String = "pat",
    impersonatedUserReference: String = DEFAULT_USER,
    auditUserReference: String = impersonatedUserReference,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((PatCreatedEventAvro.Builder) -> Unit) = {}
): EventStreamGenerator {
  require(getContext().events[asReference] == null) {
    "An event already exists for reference $asReference. Use a different reference."
  }

  val defaultAggregateModifications: ((PatCreatedEventAvro.Builder) -> Unit) = {
    setEventAuditingInformation(it.auditingInformationBuilder, auditUserReference, time)
    it.aggregateIdentifierBuilder =
        AggregateIdentifierAvro.newBuilder()
            .setIdentifier(randomUUID().toString())
            .setType(PAT.value)
            .setVersion(0)
    it.impersonatedUser =
        AggregateReferenceAvro.newBuilder()
            .setIdentifier(getByReference(impersonatedUserReference).identifier)
            .setType(UsermanagementAggregateTypeEnum.USER.value)
            .build()
    it.type = RMSPAT1
    it.scopes = listOf(GRAPHQL_API_READ, TIMELINE_API_READ)
    it.description = "A Personal Access Token for some test"
    it.hash = BCrypt.hashpw(randomUUID().toString(), BCrypt.gensalt())
    it.issuedAt = now().toEpochMilli()
    it.expiresAt = now().plusMinutes(60).toEpochMilli()
  }

  val event =
      PatCreatedEventAvro.newBuilder()
          .apply { defaultAggregateModifications(this) }
          .apply { aggregateModifications(this) }
          .build()

  sendEvent(asReference, getByReference(impersonatedUserReference), event, time)
  return this
}

@JvmOverloads
@Suppress("unused")
fun EventStreamGenerator.submitPatUpdated(
    asReference: String = "pat",
    impersonatedUserReference: String = DEFAULT_USER,
    auditUserReference: String = impersonatedUserReference,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((PatUpdatedEventAvro.Builder) -> Unit) = {}
): EventStreamGenerator {

  val defaultAggregateModifications: ((PatUpdatedEventAvro.Builder) -> Unit) = {
    setEventAuditingInformation(it.auditingInformationBuilder, auditUserReference, time)

    val previousAggregate = getPatEventOrFail(asReference)

    it.aggregateIdentifierBuilder =
        AggregateIdentifierAvro.newBuilder(previousAggregate.getAggregateIdentifier())
            .increase("IGNORE_TYPE")
    it.description = previousAggregate.getDescription()
    it.expiresAt = previousAggregate.getExpiresAt()
    it.scopes = checkNotNull(previousAggregate.getScopes())
  }

  val event =
      PatUpdatedEventAvro.newBuilder()
          .apply { defaultAggregateModifications(this) }
          .apply { aggregateModifications(this) }
          .build()

  sendEvent(asReference, getByReference(impersonatedUserReference), event, time)
  return this
}

@Suppress("unused")
fun EventStreamGenerator.submitPatDeleted(
    reference: String = "pat",
): EventStreamGenerator {
  val rootContextIdentifier =
      getEventField<String>(reference, "impersonatedUser", "identifier").toUUID()
  val aggregateIdentifier = getByReference(reference)
  val maxVersion = aggregateIdentifier.version

  for (version in 0..maxVersion) {

    sendTombstoneMessage(
        "pat",
        reference,
        AggregateEventMessageKey(
            aggregateIdentifier = aggregateIdentifier.buildAggregateIdentifier(version),
            rootContextIdentifier = rootContextIdentifier,
        ))
  }
  return this
}

private fun EventStreamGenerator.sendEvent(
    asReference: String,
    rootContextIdentifier: AggregateIdentifierAvro,
    event: SpecificRecordBase,
    time: Instant
) {
  val sentEvent =
      send(
          "pat",
          asReference,
          AggregateEventMessageKey(
              aggregateIdentifier = event.getAggregateIdentifier().buildAggregateIdentifier(),
              rootContextIdentifier = rootContextIdentifier.identifier.toUUID()),
          event,
          time.toEpochMilli())
          as SpecificRecordBase
  getContext().events[asReference] = sentEvent
  getContext().lastRootContextIdentifier = rootContextIdentifier
}

private fun EventStreamGenerator.getPatEventOrFail(asReference: String) =
    get<SpecificRecordBase>(asReference)
        ?: error("No event found for reference $asReference. Submit a pat created event first.")

private fun SpecificRecordBase.getAggregateIdentifier() =
    this.get("aggregateIdentifier") as AggregateIdentifierAvro

private fun SpecificRecordBase.getDescription() = this.get("description") as String

private fun SpecificRecordBase.getExpiresAt() = this.get("expiresAt") as Long

@Suppress("UNCHECKED_CAST")
private fun SpecificRecordBase.getScopes() = this.get("scopes") as List<PatScopeEnumAvro>
