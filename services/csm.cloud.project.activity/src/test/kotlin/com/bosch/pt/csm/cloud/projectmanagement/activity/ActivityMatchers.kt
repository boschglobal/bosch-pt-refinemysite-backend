/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity

import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.ObjectReferenceDto
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.SummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.matchesPattern
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

fun ResultActions.andExpectOk(): ResultActions = this.andExpect(status().isOk)

fun ResultActions.andExpectNotFound(): ResultActions = this.andExpect(status().isNotFound)

object ActivityMatchers {

  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

  fun hasId(activityIdentifier: UUID?, index: Int = 0): ResultMatcher =
      jsonPath("$.activities[$index].id", `is`(activityIdentifier.toString()))

  fun hasDate(instant: Instant, index: Int = 0): ResultMatcher =
      jsonPath("$.activities[$index].date", `is`(toDateString(instant)))

  fun hasUser(
      user: UserAggregateAvro,
      index: Int = 0,
  ) =
      ResultMultiMatcher(
          jsonPath("$.activities[$index].user.displayName", isDisplayNameOf(user)),
          jsonPath("$.activities[$index].user.id", `is`(user.getIdentifier().toString())),
          jsonPath("$.activities[$index].user.picture", isDefaultPicture()))

  fun hasSummary(summary: SummaryDto, index: Int = 0) =
      ResultMultiMatcher(
          jsonPath("$.activities[$index].description.template", hasNoUnresolvedMessageArgs()),
          jsonPath(
              "$.activities[$index].description.template",
              hasOnlyExpectedPlaceholders(summary.references.keys)),
          jsonPath("$.activities[$index].description.template", `is`(summary.template)),
          jsonPath(
              "$.activities[$index].description.values.length()", `is`(summary.references.size)),
          *summary.references
              .map { ref -> hasSummaryValue(ref.key, ref.value, index) }
              .toTypedArray())

  fun hasActivitiesCount(count: Int): ResultMatcher = jsonPath("$.activities.length()", `is`(count))

  fun hasChangesCount(index: Int = 0, count: Int): ResultMatcher =
      jsonPath("$.activities[$index].changes.length()", `is`(count))

  fun hasNoChanges(index: Int = 0): ResultMatcher = jsonPath("$.activities[$index].changes").isEmpty

  fun hasChange(text: String, activityIndex: Int = 0, changeIndex: Int = 0) =
      ResultMultiMatcher(
          jsonPath(
              "$.activities[$activityIndex].changes[$changeIndex]", hasNoUnresolvedMessageArgs()),
          jsonPath("$.activities[$activityIndex].changes[$changeIndex]", hasNoPlaceholders()),
          jsonPath("$.activities[$activityIndex].changes[$changeIndex]", `is`(text)))

  fun hasMessage(message: String): ResultMatcher = jsonPath("$.message", `is`(message))

  /**
   * For a message template "Changed assignee from "{0}" to "{1}", this matcher checks that each of
   * its message arguments (i.e. {0} and {1}) are resolved. That means, each message argument must
   * have been replaced with its desired value. Unresolved message arguments should not be exposed
   * to clients.
   */
  private fun hasNoUnresolvedMessageArgs() =
      object : BaseMatcher<String>() {
        override fun describeTo(description: Description?) {
          description?.appendText("no message args (looking like this: {0}, {1}, ...)")
        }

        override fun matches(actual: Any) = !Regex("\\{[0-9]+}").matches(actual as String)
      }

  private fun hasNoPlaceholders() = hasOnlyExpectedPlaceholders(emptySet())

  /**
   * For a message template "${originator} sent the task to ${assignee}", this matcher checks that
   * each of its placeholders (i.e. ${originator} and ${assignee}) are given in the map of expected
   * placeholders.
   *
   * An unexpected placeholder indicates that the test is incomplete and misses to expect that
   * placeholder.
   */
  private fun hasOnlyExpectedPlaceholders(expectedPlaceholders: Set<String>) =
      object : BaseMatcher<String>() {
        lateinit var usedButUnexpectedPlaceholders: Set<String>
        lateinit var expectedButUnusedPlaceholders: Set<String>

        override fun describeTo(description: Description?) {
          val expected = expectedPlaceholders.joinToString { "\${$it}" }
          description?.appendText("a string containing exactly these placeholders: $expected. ")
          if (usedButUnexpectedPlaceholders.isNotEmpty()) {
            description?.appendText(
                "Used in template but unexpected by test: $usedButUnexpectedPlaceholders. ")
          }
          if (expectedButUnusedPlaceholders.isNotEmpty()) {
            description?.appendText(
                "Expected by test but not used in template: $expectedButUnusedPlaceholders. ")
          }
        }

        override fun matches(actual: Any): Boolean {
          val actualPlaceholders =
              Regex("\\\$\\{([a-zA-Z0-9]+)}")
                  .findAll(actual as String)
                  .map { it.groupValues[1] }
                  .toSet()

          usedButUnexpectedPlaceholders = actualPlaceholders.minus(expectedPlaceholders)
          expectedButUnusedPlaceholders = expectedPlaceholders.minus(actualPlaceholders)

          return usedButUnexpectedPlaceholders.isEmpty() && expectedButUnusedPlaceholders.isEmpty()
        }
      }

  fun hasValidPrevLink(activityIdentifiers: UUID, expectedApiVersion: Int): ResultMatcher =
      jsonPath("$._links.prev.href", isPrevLink(activityIdentifiers.toString(), expectedApiVersion))

  private fun hasSummaryValue(key: String, value: ObjectReferenceDto, index: Int) =
      ResultMultiMatcher(
          jsonPath("$.activities[$index].description.values.$key.type", `is`(value.type)),
          jsonPath("$.activities[$index].description.values.$key.id", `is`(value.id.toString())),
          jsonPath("$.activities[$index].description.values.$key.text", `is`(value.text)))

  private fun isDisplayNameOf(user: UserAggregateAvro): Matcher<String> = `is`(user.displayName())

  private fun isDefaultPicture(): Matcher<String> = matchesPattern(".*/default-profile-picture.png")

  private fun isPrevLink(activityIdentifier: String, expectedApiVersion: Int): Matcher<String> =
      matchesPattern(
          ".*/v$expectedApiVersion/projects/tasks/[0-9a-f-]+/activities\\?before=$activityIdentifier&limit=[0-9]+")

  private fun toDateString(instant: Instant) =
      dateTimeFormatter
          .format(ZonedDateTime.from(instant.atZone(ZoneId.of("UTC"))))
          .replace("Z", "+00:00")

  class ResultMultiMatcher(private vararg val matchers: ResultMatcher) : ResultMatcher {
    override fun match(result: MvcResult) {
      for (matcher in matchers) matcher.match(result)
    }
  }
}
