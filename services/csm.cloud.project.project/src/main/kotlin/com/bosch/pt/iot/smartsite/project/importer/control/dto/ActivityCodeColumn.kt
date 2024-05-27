/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.control.dto

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.ACTIVITY_CODE
import net.sf.mpxj.ActivityCode

class ActivityCodeColumn(name: String, val code: ActivityCode) : ImportColumn(name, ACTIVITY_CODE)
