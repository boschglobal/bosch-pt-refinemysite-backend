/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro.newBuilder
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureCreatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDeletedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDisabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureEnabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureWhitelistActivatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.SubjectAddedToWhitelistEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.SubjectDeletedFromWhitelistEventAvro
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeaturetogglemanagementAggregateTypeEnum.FEATURE_TOGGLE
import java.time.Instant
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecordBase

@JvmOverloads
fun EventStreamGenerator.createFeature(
    asReference: String,
    auditUserReference: String = DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((FeatureCreatedEventAvro.Builder) -> Unit)
): EventStreamGenerator {

  val defaultAggregateModifications: ((FeatureCreatedEventAvro.Builder) -> Unit) = {
    setEventAuditingInformation(it.auditingInformationBuilder, auditUserReference, time)
    it.aggregateIdentifierBuilder =
        newBuilder()
            .setIdentifier(randomUUID().toString())
            .setType(FEATURE_TOGGLE.name)
            .setVersion(0)
  }

  val event =
      FeatureCreatedEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications.invoke(this) }
          .build()

  sendEvent(asReference, event, time)
  return this
}

@JvmOverloads
fun EventStreamGenerator.enableFeature(
    asReference: String,
    auditUserReference: String = DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((FeatureEnabledEventAvro.Builder) -> Unit)
): EventStreamGenerator {

  val previousAggregateIdentifier =
      checkNotNull(get<SpecificRecordBase>(asReference)).get("aggregateIdentifier")
          as AggregateIdentifierAvro

  val defaultAggregateModifications: ((FeatureEnabledEventAvro.Builder) -> Unit) = {
    setEventAuditingInformation(it.auditingInformationBuilder, auditUserReference, time)
    it.aggregateIdentifierBuilder = newBuilder(previousAggregateIdentifier).increase("IGNORE_TYPE")
  }

  val event =
      FeatureEnabledEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications.invoke(this) }
          .build()

  sendEvent(asReference, event, time)
  return this
}

@JvmOverloads
fun EventStreamGenerator.disableFeature(
    asReference: String,
    auditUserReference: String = DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((FeatureDisabledEventAvro.Builder) -> Unit)
): EventStreamGenerator {

  val previousAggregateIdentifier =
      checkNotNull(get<SpecificRecordBase>(asReference)).get("aggregateIdentifier")
          as AggregateIdentifierAvro

  val defaultAggregateModifications: ((FeatureDisabledEventAvro.Builder) -> Unit) = {
    setEventAuditingInformation(it.auditingInformationBuilder, auditUserReference, time)
    it.aggregateIdentifierBuilder = newBuilder(previousAggregateIdentifier).increase("IGNORE_TYPE")
  }

  val event =
      FeatureDisabledEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications.invoke(this) }
          .build()

  sendEvent(asReference, event, time)
  return this
}

@JvmOverloads
fun EventStreamGenerator.activateWhitelistOfFeature(
    asReference: String,
    auditUserReference: String = DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((FeatureWhitelistActivatedEventAvro.Builder) -> Unit)
): EventStreamGenerator {

  val previousAggregateIdentifier =
      checkNotNull(get<SpecificRecordBase>(asReference)).get("aggregateIdentifier")
          as AggregateIdentifierAvro

  val defaultAggregateModifications: ((FeatureWhitelistActivatedEventAvro.Builder) -> Unit) = {
    setEventAuditingInformation(it.auditingInformationBuilder, auditUserReference, time)
    it.aggregateIdentifierBuilder = newBuilder(previousAggregateIdentifier).increase("IGNORE_TYPE")
  }

  val event =
      FeatureWhitelistActivatedEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications.invoke(this) }
          .build()

  sendEvent(asReference, event, time)
  return this
}

@JvmOverloads
fun EventStreamGenerator.deleteFeature(
    asReference: String,
    auditUserReference: String = DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((FeatureDeletedEventAvro.Builder) -> Unit)
): EventStreamGenerator {

  val previousAggregateIdentifier =
      checkNotNull(get<SpecificRecordBase>(asReference)).get("aggregateIdentifier")
          as AggregateIdentifierAvro

  val defaultAggregateModifications: ((FeatureDeletedEventAvro.Builder) -> Unit) = {
    setEventAuditingInformation(it.auditingInformationBuilder, auditUserReference, time)
    it.aggregateIdentifierBuilder = newBuilder(previousAggregateIdentifier).increase("IGNORE_TYPE")
  }

  val event =
      FeatureDeletedEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications.invoke(this) }
          .build()

  sendEvent(asReference, event, time)
  return this
}

@JvmOverloads
fun EventStreamGenerator.addSubjectToWhitelistOfFeature(
    asReference: String,
    auditUserReference: String = DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((SubjectAddedToWhitelistEventAvro.Builder) -> Unit)
): EventStreamGenerator {

  val previousAggregateIdentifier =
      checkNotNull(get<SpecificRecordBase>(asReference)).get("aggregateIdentifier")
          as AggregateIdentifierAvro

  val defaultAggregateModifications: ((SubjectAddedToWhitelistEventAvro.Builder) -> Unit) = {
    setEventAuditingInformation(it.auditingInformationBuilder, auditUserReference, time)
    it.aggregateIdentifierBuilder = newBuilder(previousAggregateIdentifier).increase("IGNORE_TYPE")
  }

  val event =
      SubjectAddedToWhitelistEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications.invoke(this) }
          .build()

  sendEvent(asReference, event, time)
  return this
}

@JvmOverloads
fun EventStreamGenerator.deleteSubjectFromWhitelistOfFeature(
    asReference: String,
    auditUserReference: String = DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
    aggregateModifications: ((SubjectDeletedFromWhitelistEventAvro.Builder) -> Unit)
): EventStreamGenerator {

  val previousAggregateIdentifier =
      checkNotNull(get<SpecificRecordBase>(asReference)).get("aggregateIdentifier")
          as AggregateIdentifierAvro

  val defaultAggregateModifications: ((SubjectDeletedFromWhitelistEventAvro.Builder) -> Unit) = {
    setEventAuditingInformation(it.auditingInformationBuilder, auditUserReference, time)
    it.aggregateIdentifierBuilder = newBuilder(previousAggregateIdentifier).increase("IGNORE_TYPE")
  }

  val event =
      SubjectDeletedFromWhitelistEventAvro.newBuilder()
          .apply { defaultAggregateModifications.invoke(this) }
          .apply { aggregateModifications.invoke(this) }
          .build()

  sendEvent(asReference, event, time)
  return this
}

private fun EventStreamGenerator.sendEvent(
    asReference: String,
    event: SpecificRecordBase,
    time: Instant
) {
  val sentEvent =
      send("feature", asReference, null, event, time.toEpochMilli()) as SpecificRecordBase
  getContext().events[asReference] = sentEvent
  getContext().lastRootContextIdentifier =
      sentEvent.get("aggregateIdentifier") as AggregateIdentifierAvro
}
