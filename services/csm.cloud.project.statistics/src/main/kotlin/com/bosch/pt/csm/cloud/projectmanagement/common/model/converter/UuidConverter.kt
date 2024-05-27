/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.common.model.converter

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import org.apache.commons.lang3.StringUtils
import java.util.UUID
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/** Converter for [UUIDs][UUID] used in [entities][Entity]. */
@Converter(autoApply = true)
class UuidConverter : AttributeConverter<UUID?, String?> {
  override fun convertToDatabaseColumn(attribute: UUID?) = attribute?.toString()

  override fun convertToEntityAttribute(dbData: String?) =
      if (StringUtils.isNotEmpty(dbData)) dbData!!.toUUID() else null
}
