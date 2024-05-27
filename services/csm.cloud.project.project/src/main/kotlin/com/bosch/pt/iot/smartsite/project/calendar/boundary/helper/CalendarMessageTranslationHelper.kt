/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.helper

import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.common.i18n.Key.CALENDAR_EMPTY
import com.bosch.pt.iot.smartsite.common.i18n.Key.CALENDAR_EXPORT_DATE
import com.bosch.pt.iot.smartsite.common.i18n.Key.CALENDAR_HAS_FILTERS_APPLIED
import com.bosch.pt.iot.smartsite.common.i18n.Key.CALENDAR_WEEK_DAY_HEADER_PATTERN
import com.bosch.pt.iot.smartsite.common.i18n.Key.CALENDAR_WEEK_DAY_MILESTONE_HEADER_PATTERN
import com.bosch.pt.iot.smartsite.common.i18n.Key.CALENDAR_WEEK_HEADER_PATTERN
import com.bosch.pt.iot.smartsite.common.i18n.Key.CALENDAR_WEEK_NAME
import java.time.Instant
import java.util.Date
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder.getLocale
import org.springframework.stereotype.Component

@Component
class CalendarMessageTranslationHelper(private val messageSource: MessageSource) {

  /*
   * In the case includeDayCards is true, is irrelevant if includeMilestones is true or not
   * because day cards always take priority in the week day header style, so the includeDayCards
   * check in when() should always be on top.
   */
  fun getWeekDayCellPattern(includeDayCards: Boolean, includeMilestones: Boolean): String =
      when {
        includeDayCards ->
            messageSource.getMessage(CALENDAR_WEEK_DAY_HEADER_PATTERN, arrayOf(), getLocale())
        includeMilestones ->
            messageSource.getMessage(
                CALENDAR_WEEK_DAY_MILESTONE_HEADER_PATTERN, arrayOf(), getLocale())
        else -> ""
      }

  fun getWeekCellPattern(): String =
      messageSource.getMessage(CALENDAR_WEEK_HEADER_PATTERN, arrayOf(), getLocale())

  fun getWeekCellName(): String =
      messageSource.getMessage(CALENDAR_WEEK_NAME, arrayOf(), getLocale())

  fun getWeekRowName(): String =
      messageSource.getMessage(Key.CALENDAR_WEEK_HEADER, arrayOf(), getLocale())

  fun getGlobalMilestoneName(): String =
      messageSource.getMessage(Key.CALENDAR_MILESTONE_HEADER, arrayOf(), getLocale())

  fun getNoWorkAreaName(): String =
      messageSource.getMessage(Key.CALENDAR_WITHOUT_WORKAREA_HEADER, arrayOf(), getLocale())

  fun getInvestorMilestoneName(): String =
      messageSource.getMessage(Key.CALENDAR_MILESTONE_TYPE_ENUM_INVESTOR, arrayOf(), getLocale())

  fun getProjectMilestoneName(): String =
      messageSource.getMessage(Key.CALENDAR_MILESTONE_TYPE_ENUM_PROJECT, arrayOf(), getLocale())

  fun getCalendarExportName(): String =
      messageSource.getMessage(CALENDAR_EXPORT_DATE, arrayOf(Date.from(Instant.now())), getLocale())

  fun getCalendarFilterAppliedName(): String =
      messageSource.getMessage(CALENDAR_HAS_FILTERS_APPLIED, arrayOf(), getLocale())

  fun getCalendarLegendEmptyName(): String =
      messageSource.getMessage(CALENDAR_EMPTY, arrayOf(), getLocale())
}
