/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model.converter

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.UUID

@Converter
class UuidConverter : AttributeConverter<UUID?, String?> {

  override fun convertToDatabaseColumn(attribute: UUID?): String? = attribute?.toString()

  override fun convertToEntityAttribute(dbData: String?): UUID? =
      if (dbData.isNullOrEmpty()) null else dbData.toUUID()
}
