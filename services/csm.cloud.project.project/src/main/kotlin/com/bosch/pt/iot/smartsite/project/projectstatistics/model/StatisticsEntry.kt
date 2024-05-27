/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectstatistics.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.Objects

/**
 * A statistics entry to be used with JPA's new operator for capturing the result of an in-database
 * statistics calculation (like a COUNT as illustrated below). The calculation is assumed to be of
 * the following form:
 *
 * <pre>`SELECT NEW MyStatisticsEntry(COUNT(e.level), e.level, e.parent.identifier) FROM MyEntity e
 * (...) GROUP BY e.level, e.parent.identifier `</pre> *
 *
 * Notice how the query is grouped by `e.level` so that the aggregation (here COUNT) is taking all
 * occurrences of that level as input, yielding a single statistics value for each level.
 *
 * @param <T> the type of the grouping levels
 * @param <U> the type of the statistics value </U></T>
 */
@Suppress("UnnecessaryAbstractClass")
abstract class StatisticsEntry<T, U>(

    /** the statistics value (count, sum, ...) calculated for the grouping level */
    val statistics: U,

    /** the level of the grouped attribute */
    val level: T,

    /**
     * the identifier of the entity holding the grouped attribute, or the identifier of the entity
     * that has been used for grouping.
     */
    val entityIdentifier: ProjectId
) {

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as StatisticsEntry<*, *>
    return (level == that.level &&
        statistics == that.statistics &&
        entityIdentifier == that.entityIdentifier)
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int = Objects.hash(level, statistics, entityIdentifier)

  companion object {

    /**
     * Converts the given statistics entries to a map of maps, where the outer map maps entities to
     * their statistics entries, and the inner map maps each grouping level to its associated
     * statistics.
     */
    fun <T, U> toMap(statisticsEntries: List<StatisticsEntry<T, U>>): Map<ProjectId, Map<T, U>> =
        statisticsEntries.fold(HashMap<ProjectId, MutableMap<T, U>>()) { map, statisticsEntry ->
          if (!map.contains(statisticsEntry.entityIdentifier)) {
            map[statisticsEntry.entityIdentifier] = HashMap()
          }
          map[statisticsEntry.entityIdentifier]!![statisticsEntry.level] =
              statisticsEntry.statistics
          map
        }
  }
}
