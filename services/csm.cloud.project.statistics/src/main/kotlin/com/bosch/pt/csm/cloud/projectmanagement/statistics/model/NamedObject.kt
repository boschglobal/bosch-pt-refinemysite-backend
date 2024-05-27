/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    indexes =
        [Index(name = "UK_NamedObject_identifier", columnList = "type,identifier", unique = true)])
class NamedObject(type: String, identifier: UUID, var name: String) :
    AbstractPersistable<Long>(), Referable {

  @Embedded private var objectIdentifier = ObjectIdentifier(type, identifier)

  override fun getDisplayName() = name

  override fun getIdentifierUuid(): UUID = objectIdentifier.identifier
}
