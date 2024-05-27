/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountGroupedEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardReasonCountEntry
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardReasonCountGroupedEntry
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DayCardStatisticsRepository : JpaRepository<DayCard, Long> {

  @Query(
      "SELECT new com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountEntry(" +
          "status, COUNT(daycard.id), FLOOR(DATEDIFF(daycard.date, :startDate1)/7)) " +
          "FROM DayCard daycard " +
          "WHERE daycard.projectIdentifier = :projectIdentifier " +
          "AND daycard.date BETWEEN :startDate2 AND :endDate " +
          "GROUP BY FLOOR(DATEDIFF(daycard.date, :startDate)/7), daycard.status")
  fun getDayCardCountPerStatusAndWeekWithinDates(
      @Param("projectIdentifier") projectIdentifier: UUID,
      @Param("startDate") startDate: LocalDate,
      @Param("startDate1") startDate1: LocalDate,
      @Param("startDate2") startDate2: LocalDate,
      @Param("endDate") endDate: LocalDate
  ): List<DayCardCountEntry>

  @Query(
      "SELECT new com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardCountGroupedEntry(" +
          "daycard.status, COUNT(daycard.id), FLOOR(DATEDIFF(daycard.date, :startDate)/7), " +
          "participant.companyIdentifier, daycard.craftIdentifier) " +
          "FROM DayCard daycard LEFT JOIN daycard.assignedParticipant participant " +
          "WHERE daycard.projectIdentifier = :projectIdentifier " +
          "AND daycard.date BETWEEN :startDate AND :endDate " +
          "GROUP BY FLOOR(DATEDIFF(daycard.date, :startDate)/7), daycard.status, " +
          "participant.companyIdentifier, daycard.craftIdentifier")
  fun getDayCardCountPerStatusAndWeekWithinDatesGrouped(
      @Param("projectIdentifier") projectIdentifier: UUID,
      @Param("startDate") startDate: LocalDate,
      @Param("endDate") endDate: LocalDate
  ): List<DayCardCountGroupedEntry>

  @Query(
      "SELECT new com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure.DayCardReasonCountEntry(" +
          "daycard.reason, COUNT(daycard.id), FLOOR(DATEDIFF(daycard.date, :startDate)/7)) " +
          "FROM DayCard daycard " +
          "WHERE daycard.projectIdentifier = :projectIdentifier " +
          "AND daycard.status = " +
          "com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum.NOTDONE " +
          "AND daycard.date BETWEEN :startDate AND :endDate " +
          "GROUP BY FLOOR(DATEDIFF(daycard.date, :startDate)/7), daycard.reason")
  fun getDayCardCountPerReasonAndWeekWithinDates(
      @Param("projectIdentifier") projectIdentifier: UUID,
      @Param("startDate") startDate: LocalDate,
      @Param("endDate") endDate: LocalDate
  ): List<DayCardReasonCountEntry>

  @Query(
      "SELECT new com.bosch.pt.csm.cloud.projectmanagement.statistics.model.datastructure." +
          "DayCardReasonCountGroupedEntry(" +
          "daycard.reason, COUNT(daycard.id), FLOOR(DATEDIFF(daycard.date, :startDate)/7), " +
          "participant.companyIdentifier, daycard.craftIdentifier) " +
          "FROM DayCard daycard LEFT JOIN daycard.assignedParticipant participant " +
          "WHERE daycard.projectIdentifier = :projectIdentifier " +
          "AND daycard.status = " +
          "com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum.NOTDONE " +
          "AND daycard.date BETWEEN :startDate AND :endDate " +
          "GROUP BY FLOOR(DATEDIFF(daycard.date, :startDate)/7), daycard.reason, " +
          "participant.companyIdentifier, daycard.craftIdentifier")
  fun getDayCardCountPerReasonAndWeekWithinDatesGrouped(
      @Param("projectIdentifier") projectIdentifier: UUID,
      @Param("startDate") startDate: LocalDate,
      @Param("endDate") endDate: LocalDate
  ): List<DayCardReasonCountGroupedEntry>
}
