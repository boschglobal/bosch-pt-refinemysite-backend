/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.model.converter

import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.model.Sortable
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class AnnouncementTypeConverter : AttributeConverter<AnnouncementTypeEnum?, Int?> {

  override fun convertToDatabaseColumn(attribute: AnnouncementTypeEnum?): Int? =
      attribute?.getPosition()

  override fun convertToEntityAttribute(dbData: Int?): AnnouncementTypeEnum =
      dbData!!.let { Sortable[AnnouncementTypeEnum::class.java, dbData] }
}
