/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.model.converter

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import java.util.UUID
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/** Converter for [UUIDs][UUID] used in [entities][Entity]. */
@Converter(autoApply = true)
class UuidConverter : AttributeConverter<UUID, String> {

  override fun convertToDatabaseColumn(attribute: UUID?): String? = attribute?.toString()

  override fun convertToEntityAttribute(dbData: String?): UUID? = dbData?.toUUID()
}
