/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import jakarta.persistence.AttributeOverride
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Entity
@Table(name = "PAT_ENTITY", indexes = [Index(name = "UK_Hash", columnList = "hash", unique = true)])
class Pat : AbstractSnapshotEntity<Long, PatId>() {

  @Column(length = MAX_TYPE_LENGTH, nullable = false, columnDefinition = "varchar(8)") // type field
  @Enumerated(EnumType.STRING)
  lateinit var type: PatTypeEnum

  @field:Size(max = MAX_DESCRIPTION_LENGTH) // description field
  @Column(length = MAX_DESCRIPTION_LENGTH, nullable = false)
  lateinit var description: String

  @field:Size(min = 1) // scopes field
  @ElementCollection(fetch = EAGER)
  @CollectionTable(
      name = "PAT_SCOPE",
      foreignKey = ForeignKey(name = "FK_PAT_Scope_PatId"),
      joinColumns = [JoinColumn(name = "PAT_ID")])
  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "varchar(32)")
  lateinit var scopes: MutableList<PatScopeEnum>

  @field:Size(max = MAX_HASH_LENGTH) // hash field
  @Column(length = MAX_HASH_LENGTH, nullable = false)
  lateinit var hash: String

  @Embedded // impersonatedUser field
  @AttributeOverride(
      name = "identifier",
      column = Column(nullable = false, length = 36, name = "impersonatedUser"))
  lateinit var impersonatedUser: UserId

  @Column(nullable = false) // issuedAt field
  lateinit var issuedAt: LocalDateTime

  @Column(nullable = false) // expiresAt field
  lateinit var expiresAt: LocalDateTime

  override fun getDisplayName(): String = this.identifier.toString().replace("-", "")

  companion object {
    private const val MAX_TYPE_LENGTH = 8
    private const val MAX_DESCRIPTION_LENGTH = 128
    private const val MAX_HASH_LENGTH = 512
  }
}
