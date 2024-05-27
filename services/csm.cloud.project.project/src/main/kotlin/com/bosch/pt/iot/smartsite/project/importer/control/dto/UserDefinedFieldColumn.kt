/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.control.dto

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.USER_DEFINED_FIELD
import net.sf.mpxj.FieldType

class UserDefinedFieldColumn(name: String, fieldType: FieldType) :
    ImportColumn(name, USER_DEFINED_FIELD, fieldType)
