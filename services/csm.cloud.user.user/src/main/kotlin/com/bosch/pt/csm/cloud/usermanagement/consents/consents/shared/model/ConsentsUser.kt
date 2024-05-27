/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.model

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import java.time.LocalDateTime
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "CONSENTS_USER",
    indexes =
        [
            Index(name = "IX_ConsentsUser_Identifier", columnList = "identifier", unique = true),
        ])
class ConsentsUser(
    @Column(nullable = false) var delayedAt: LocalDateTime,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "USER_CONSENT",
        foreignKey = ForeignKey(name = "FK_UserConsent_ConsentsUser"),
    )
    val consents: MutableCollection<UserConsent>
) : AbstractSnapshotEntity<Long, UserId>() {

  // TODO: move this later to query view (if needed at all)
  override fun getDisplayName(): String? {
    TODO("Not yet implemented")
  }
}
