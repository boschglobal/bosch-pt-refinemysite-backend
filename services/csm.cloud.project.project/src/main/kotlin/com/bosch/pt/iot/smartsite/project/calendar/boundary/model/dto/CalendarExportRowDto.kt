/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.dto

import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date
import java.util.UUID

data class CalendarExportRowDto(

    // Task Information
    val taskIdentifier: TaskId? = null,
    val taskName: String? = null,
    val taskStatus: String? = null,
    val taskDescription: String? = null,
    val taskLocation: String? = null,
    val taskCraftName: String? = null,
    val taskWorkAreaName: String? = null,
    val taskAssigneeUserName: String? = null,
    val taskAssigneeCompanyName: String? = null,

    // Task Schedule Information
    val taskStartDate: LocalDate? = null,
    val taskEndDate: LocalDate? = null,

    // Task Constraints
    val taskConstraintCommonUnderstanding: Boolean? = null,
    val taskConstraintEquipment: Boolean? = null,
    val taskConstraintExternalFactors: Boolean? = null,
    val taskConstraintInformation: Boolean? = null,
    val taskConstraintMaterial: Boolean? = null,
    val taskConstraintPreliminaryWork: Boolean? = null,
    val taskConstraintResources: Boolean? = null,
    val taskConstraintSafeWorkingEnvironment: Boolean? = null,
    val taskConstraintCustom1: Boolean? = null,
    val taskConstraintCustom2: Boolean? = null,
    val taskConstraintCustom3: Boolean? = null,
    val taskConstraintCustom4: Boolean? = null,
    var dayCardIdentifier: DayCardId? = null,
    val dayCardVersion: Long? = null,
    var dayCardTitle: String? = null,
    var dayCardManpower: BigDecimal? = null,
    var dayCardNotes: String? = null,
    var dayCardDate: LocalDate? = null,
    var dayCardStatus: String? = null,
    var dayCardReason: String? = null,

    // Day card created by user information
    var dayCardCreatedByIdentifier: UUID? = null,
    var dayCardCreatedByUserName: String? = null,
    var dayCardCreatedByCompanyName: String? = null,
    var dayCardCreatedAtTimestamp: Date? = null,

    // Day card modified by user information
    var dayCardLastModifiedByIdentifier: UUID? = null,
    var dayCardLastModifiedByUserName: String? = null,
    var dayCardLastModifiedByCompanyName: String? = null,
    var dayCardLastModifiedAtTimestamp: Date? = null
)
