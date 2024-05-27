/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.common.model.converter

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import java.util.UUID
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class UuidConverter : AttributeConverter<UUID?, String?> {

  override fun convertToDatabaseColumn(attribute: UUID?): String? = attribute?.toString()

  override fun convertToEntityAttribute(dbData: String?): UUID? =
      if (dbData.isNullOrBlank()) {
        null
      } else dbData.toUUID()
}
