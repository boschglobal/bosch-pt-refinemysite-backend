/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID
import jakarta.persistence.Embeddable

@Embeddable
data class DocumentVersionId(@get:JsonValue override val identifier: UUID = randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString() = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 1L
  }
}

@Embeddable
data class DocumentId(@get:JsonValue override val identifier: UUID = randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString() = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 1L
  }
}

data class DocumentVersion(val identifier: DocumentVersionId, val lastChanged: LocalDateTime)

enum class DocumentType {
  TERMS_AND_CONDITIONS,
  EULA
}

enum class Client {
  MOBILE,
  WEB
}

enum class ClientSet(val specificity: Int, private val elements: Set<Client>) {
  ALL(0, setOf(Client.MOBILE, Client.WEB)),
  MOBILE(10, setOf(Client.MOBILE)),
  WEB(10, setOf(Client.WEB));

  operator fun contains(client: Client) = client in elements
}
