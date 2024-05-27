/*
 * **************** ********************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID
import java.util.UUID.randomUUID

@Embeddable
@Suppress("SerialVersionUIDInSerializableClass")
data class PatId(
    @get:JsonValue @Column(nullable = false) override val identifier: UUID = randomUUID()
) : Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  override fun toUuid(): UUID = identifier
}

fun UUID.asPatId() = PatId(this)
