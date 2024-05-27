/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.model.converter

import com.bosch.pt.iot.smartsite.common.model.Sortable
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class ParticipantStatusConverter : AttributeConverter<ParticipantStatusEnum?, Int?> {

  override fun convertToDatabaseColumn(attribute: ParticipantStatusEnum?): Int? =
      attribute?.getPosition()

  override fun convertToEntityAttribute(dbData: Int?): ParticipantStatusEnum =
      Sortable[ParticipantStatusEnum::class.java, dbData]
}
