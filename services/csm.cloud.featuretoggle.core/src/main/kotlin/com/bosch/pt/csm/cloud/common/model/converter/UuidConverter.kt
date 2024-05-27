/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.model.converter

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import java.util.UUID
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.apache.commons.lang3.StringUtils

@Converter(autoApply = true)
class UuidConverter : AttributeConverter<UUID?, String?> {
  override fun convertToDatabaseColumn(attribute: UUID?) = attribute?.toString()

  override fun convertToEntityAttribute(dbData: String?) =
      if (StringUtils.isNotEmpty(dbData)) dbData!!.toUUID() else null
}
