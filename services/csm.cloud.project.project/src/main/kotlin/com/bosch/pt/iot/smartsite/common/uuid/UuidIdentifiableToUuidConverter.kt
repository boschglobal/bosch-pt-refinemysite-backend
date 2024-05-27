/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.uuid

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import java.util.UUID
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class UuidIdentifiableToUuidConverter : Converter<UuidIdentifiable, UUID> {

  override fun convert(source: UuidIdentifiable) = source.toUuid()
}
