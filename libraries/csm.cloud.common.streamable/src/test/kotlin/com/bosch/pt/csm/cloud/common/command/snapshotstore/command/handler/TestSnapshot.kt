/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore.command.handler

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import java.time.LocalDateTime
import java.util.UUID

data class TestSnapshot(
    override val identifier: UserId,
    override val version: Long,
    override val createdDate: LocalDateTime?,
    override val lastModifiedDate: LocalDateTime?,
    override val createdBy: UserId?,
    override val lastModifiedBy: UserId?,
    val rootContextIdentifier: UUID,
    var name: String = "test",
) : VersionedSnapshot, AuditableSnapshot

fun TestSnapshot.toCommandHandler() = CommandHandler.of(this, TestSnapshotMapper)
