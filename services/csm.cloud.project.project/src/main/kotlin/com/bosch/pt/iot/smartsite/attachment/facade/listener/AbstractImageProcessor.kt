/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.attachment.facade.listener

import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractImageProcessor {

  /** Matches all [vars] against the matcher groups by index */
  protected fun pathMatches(matcher: Matcher, vararg vars: String?): Boolean {
    vars.forEachIndexed { index, identifier ->
      val group = index + 1
      if (matcher.group(group) != identifier) {
        return false
      }
    }
    return true
  }

  protected fun logNotFound(type: String, ownerIdentifier: UUID) {
    LOGGER.warn("$type with id $ownerIdentifier not found to update available resolutions")
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ImageScalingProcessor::class.java)

    @JvmStatic
    protected val PROJECT_CONTEXT_PICTURE_PATTERN: Pattern = Pattern.compile("/images/projects/.*")
    @JvmStatic
    protected val PROJECT_PICTURE_PATTERN: Pattern =
        Pattern.compile("/images/projects/([^/]*)/picture")
    @JvmStatic
    protected val MESSAGE_ATTACHMENT_PATTERN: Pattern =
        Pattern.compile("/images/projects/([^/]*)/tasks/([^/]*)/topics/([^/]*)/messages/([^/]*)")
    @JvmStatic
    protected val TOPIC_ATTACHMENT_PATTERN: Pattern =
        Pattern.compile("/images/projects/([^/]*)/tasks/([^/]*)/topics/([^/]*)")
    @JvmStatic
    protected val TASK_ATTACHMENT_PATTERN: Pattern =
        Pattern.compile("/images/projects/([^/]*)/tasks/([^/]*)")
  }
}
