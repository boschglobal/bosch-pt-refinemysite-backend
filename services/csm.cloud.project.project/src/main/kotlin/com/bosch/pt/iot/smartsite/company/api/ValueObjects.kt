/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

@file:Suppress("MatchingDeclarationName")

package com.bosch.pt.iot.smartsite.company.api

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.UUID
import java.util.UUID.randomUUID
import jakarta.persistence.Embeddable

@Embeddable
data class CompanyId(@get:JsonValue override val identifier: UUID = randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 8723827381254L
  }
}

fun UUID.asCompanyId() = CompanyId(this)

fun String.asCompanyId() = CompanyId(this)

fun CompanyId.toAggregateReference(): AggregateIdentifierAvro =
    AggregateIdentifierAvro.newBuilder()
        .setType(CompanymanagementAggregateTypeEnum.COMPANY.name)
        .setIdentifier(this.identifier.toString())
        .setVersion(0)
        .build()
