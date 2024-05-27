/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.control.dto

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.CUSTOM_FIELD
import net.sf.mpxj.FieldType

class CustomFieldColumn(name: String, fieldType: FieldType) :
    ImportColumn(name, CUSTOM_FIELD, fieldType)
