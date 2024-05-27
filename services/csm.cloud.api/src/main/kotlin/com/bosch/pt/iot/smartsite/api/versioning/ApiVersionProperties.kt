/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.versioning

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.format.annotation.DateTimeFormat

@Configuration
@EnableConfigurationProperties(ApiVersionProperties::class)
class ApiPropertiesConfiguration

@ConfigurationProperties(prefix = "custom.api")
data class ApiVersionProperties(
    val graphql: Version = Version(),
    val timeline: TimelineApiVersionProperties = TimelineApiVersionProperties(),
    val internal: InternalApiVersionProperties = InternalApiVersionProperties()
)

data class TimelineApiVersionProperties(
    val authenticationStatus: Version = Version(),
    val company: Version = Version(),
    val project: Version = Version(),
    val translation: Version = Version(),
    val user: Version = Version()
)

data class InternalApiVersionProperties(
    val announcement: Version = Version(),
    val bimModel: Version = Version(),
    val craft: Version = Version(),
    val company: Version = Version(),
    val documents: Version = Version(),
    val event: Version = Version(),
    val feature: Version = Version(),
    val job: Version = Version(),
    val project: Version = Version(),
    val translation: Version = Version(),
    val user: Version = Version(),
    @JsonIgnore val unknown: Version = Version(Int.MAX_VALUE, Int.MAX_VALUE, null)
)

data class Version(
    val min: Int = 1,
    val max: Int = 1,
    val removalTimeline: Array<RemovalDate>? = null
) {

  init {
    require(min > 0) { "Min must be > 0" }
    require(max > 0) { "Max must be > 0" }
    require(max >= min) { "Max must be >= min but max is $max and min is $min." }
  }

  data class RemovalDate(
      val version: Int,
      @DateTimeFormat(pattern = "yyyy-MM-dd") val date: LocalDate
  )

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Version

    if (min != other.min) return false
    if (max != other.max) return false
    if (removalTimeline != null) {
      if (other.removalTimeline == null) return false
      if (!removalTimeline.contentEquals(other.removalTimeline)) return false
    } else if (other.removalTimeline != null) return false

    return true
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int {
    var result = min
    result = 31 * result + max
    result = 31 * result + (removalTimeline?.contentHashCode() ?: 0)
    return result
  }
}
