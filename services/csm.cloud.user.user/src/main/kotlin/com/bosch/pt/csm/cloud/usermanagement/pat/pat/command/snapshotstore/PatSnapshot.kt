/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.Pat
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatTypeEnum
import java.time.LocalDateTime

data class PatSnapshot(
    override val identifier: PatId,
    override val version: Long = INITIAL_SNAPSHOT_VERSION,
    val impersonatedUser: UserId,
    var description: String,
    val hash: String,
    val type: PatTypeEnum,
    val issuedAt: LocalDateTime,
    var expiresAt: LocalDateTime,
    var scopes: List<PatScopeEnum>,
    override val createdDate: LocalDateTime? = null,
    override val createdBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
) : VersionedSnapshot, AuditableSnapshot {
  constructor(
      pat: Pat
  ) : this(
      identifier = pat.identifier,
      version = pat.version,
      impersonatedUser = pat.impersonatedUser,
      description = pat.description,
      hash = pat.hash,
      type = pat.type,
      issuedAt = pat.issuedAt,
      expiresAt = pat.expiresAt,
      scopes = pat.scopes,
  )

  fun toCommandHandler() = CommandHandler.of(this)
}

fun Pat.asSnapshot() = PatSnapshot(this)
