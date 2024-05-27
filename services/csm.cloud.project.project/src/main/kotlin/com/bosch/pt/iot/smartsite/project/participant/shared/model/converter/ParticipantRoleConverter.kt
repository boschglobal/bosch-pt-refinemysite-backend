/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.model.converter

import com.bosch.pt.iot.smartsite.common.model.Sortable
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/** Converter for [project role][ParticipantRoleEnum] used in [entities][Entity]. */
@Converter(autoApply = true)
class ParticipantRoleConverter : AttributeConverter<ParticipantRoleEnum?, Int?> {

  override fun convertToDatabaseColumn(attribute: ParticipantRoleEnum?): Int? =
      attribute?.getPosition()

  override fun convertToEntityAttribute(dbData: Int?): ParticipantRoleEnum =
      Sortable[ParticipantRoleEnum::class.java, dbData]
}
