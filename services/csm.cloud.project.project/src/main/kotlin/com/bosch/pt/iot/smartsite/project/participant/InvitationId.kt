/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.UUID
import jakarta.persistence.Embeddable

@Embeddable
data class InvitationId(@get:JsonValue override val identifier: UUID = UUID.randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 873825386254L
  }
}

fun UUID.asInvitationId() = InvitationId(this)