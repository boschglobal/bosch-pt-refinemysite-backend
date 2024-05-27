/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.model.converter

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import java.util.UUID
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.apache.commons.lang3.StringUtils.isEmpty

/** Converter for [UUIDs][UUID] used in entities. */
@Converter(autoApply = true)
class UuidConverter : AttributeConverter<UUID?, String?> {

  override fun convertToDatabaseColumn(attribute: UUID?): String? = attribute?.toString()

  override fun convertToEntityAttribute(dbData: String?): UUID? =
      if (isEmpty(dbData)) {
        null
      } else dbData!!.toUUID()
}
