/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

@file:Suppress("MatchingDeclarationName", "Filename")
/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.UUID
import jakarta.persistence.Embeddable

@Embeddable
@SuppressWarnings("SerialVersionUIDInSerializableClass")
data class CompanyId(@get:JsonValue override val identifier: UUID) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()
}

fun UUID.asCompanyId() = CompanyId(this)

data class StreetAddressVo(
    override val city: String,
    override val zipCode: String,
    override val area: String? = null,
    override val country: String,
    val street: String,
    val houseNumber: String
) : AbstractAddressVo()

data class PostBoxAddressVo(
    override val city: String,
    override val zipCode: String,
    override val area: String? = null,
    override val country: String,
    val postBox: String
) : AbstractAddressVo()

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractAddressVo {
  abstract val city: String
  abstract val zipCode: String
  abstract val area: String?
  abstract val country: String
}
