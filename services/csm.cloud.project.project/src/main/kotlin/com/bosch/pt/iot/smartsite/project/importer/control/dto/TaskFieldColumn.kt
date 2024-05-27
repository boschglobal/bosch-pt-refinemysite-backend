/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.control.dto

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.TASK_FIELD
import net.sf.mpxj.FieldType

class TaskFieldColumn(name: String, fieldType: FieldType) :
    ImportColumn(name, TASK_FIELD, fieldType)
