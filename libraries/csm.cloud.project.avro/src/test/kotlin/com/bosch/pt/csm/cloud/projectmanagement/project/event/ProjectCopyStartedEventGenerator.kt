/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.projectmanagement.copy.messages.ProjectCopyStartedEventAvro
import java.time.Instant
import java.util.UUID
import java.util.UUID.randomUUID

@JvmOverloads
fun EventStreamGenerator.submitProjectCopyStarted(
    asBusinessTransactionReference: String = "projectCopyTransaction",
    rootContextIdentifier: UUID = getContext().lastRootContextIdentifier!!.identifier.toUUID(),
    auditUserReference: String = EventStreamGenerator.DEFAULT_USER,
    time: Instant = getContext().timeLineGenerator.next(),
): EventStreamGenerator {
  val eventBuilder =
      ProjectCopyStartedEventAvro.newBuilder().apply {
        setEventAuditingInformation(auditingInformationBuilder, auditUserReference, time)
      }

  val businessTransactionIdentifier =
      registerRandomVBusinessTransactionIdentifier(asBusinessTransactionReference)

  val messageKey =
      BusinessTransactionStartedMessageKey(businessTransactionIdentifier, rootContextIdentifier)

  send(
      "project",
      asBusinessTransactionReference,
      messageKey,
      eventBuilder.build(),
      time.toEpochMilli(),
      asBusinessTransactionReference)
  getContext().lastBusinessTransactionReference = asBusinessTransactionReference

  return this
}

private fun EventStreamGenerator.registerRandomVBusinessTransactionIdentifier(asReference: String) =
    randomUUID().also { getContext().registerBusinessTransaction(asReference, it) }
