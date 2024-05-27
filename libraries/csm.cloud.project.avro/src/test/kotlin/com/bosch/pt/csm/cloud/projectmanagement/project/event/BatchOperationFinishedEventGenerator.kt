/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionFinishedMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import java.time.Instant
import java.util.UUID

@JvmOverloads
fun EventStreamGenerator.submitBatchOperationFinished(
    rootContextIdentifier: UUID = getContext().lastRootContextIdentifier!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String = getContext().lastBusinessTransactionReference!!,
): EventStreamGenerator {
  val eventBuilder =
      BatchOperationFinishedEventAvro.newBuilder().apply {
        setEventAuditingInformation(auditingInformationBuilder, auditUserReference, time)
      }

  val businessTransactionIdentifier =
      findBusinessTransactionIdentifierOrFail(businessTransactionReference)

  val messageKey =
      BusinessTransactionFinishedMessageKey(businessTransactionIdentifier, rootContextIdentifier)

  send(
      "project",
      businessTransactionReference,
      messageKey,
      eventBuilder.build(),
      time.toEpochMilli(),
      businessTransactionReference)

  return this
}

private fun EventStreamGenerator.findBusinessTransactionIdentifierOrFail(reference: String) =
    getContext().getBusinessTransaction(reference)
        ?: throw IllegalArgumentException(
            "Unable to find business transaction for reference $reference")
