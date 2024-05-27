/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.model.ConsentsUser
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import java.time.LocalDate
import java.time.LocalDateTime

data class ConsentsUserSnapshot(
    val delayedAt: LocalDateTime,
    val consents: Collection<UserConsent>,
    override val identifier: UserId,
    override val version: Long
) : VersionedSnapshot {

  fun hasGivenConsent(documentVersionId: DocumentVersionId) =
      consents.map { it.documentVersionId }.contains(documentVersionId)

  fun toCommandHandler() = CommandHandler.of(this)
}

data class UserConsent(val date: LocalDate, val documentVersionId: DocumentVersionId)

fun ConsentsUser.asValueObject() =
    ConsentsUserSnapshot(
        this.delayedAt,
        this.consents.map { UserConsent(it.date, it.documentVersionId) },
        this.identifier,
        this.version)
