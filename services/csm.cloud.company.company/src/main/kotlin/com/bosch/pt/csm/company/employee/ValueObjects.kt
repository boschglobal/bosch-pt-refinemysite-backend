@file:Suppress("MatchingDeclarationName", "Filename")
/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.UUID
import jakarta.persistence.Embeddable

@Embeddable
@SuppressWarnings("SerialVersionUIDInSerializableClass")
data class EmployeeId(@get:JsonValue override val identifier: UUID) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()
}

fun UUID.asEmployeeId() = EmployeeId(this)
