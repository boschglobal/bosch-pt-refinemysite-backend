/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.reset.api

import com.bosch.pt.iot.smartsite.reset.Resettable
import com.bosch.pt.iot.smartsite.reset.RestoreDbResettable
import com.bosch.pt.iot.smartsite.reset.boundary.KafkaSchemaRegistryResetService
import com.bosch.pt.iot.smartsite.reset.boundary.UserInitializationService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ResetController(
    private val environment: Environment,
    private val resetServices: List<Resettable>,
    private val restoreDbResettableServices: List<RestoreDbResettable>?,
    private val userInitializationService: UserInitializationService,
    private val kafkaSchemaRegistryResetService: KafkaSchemaRegistryResetService
) {

  @PostMapping("/reset/restoredb")
  fun reset() {
    resetRestoreDbs()
  }

  @PostMapping("/reset")
  fun reset(@RequestParam(required = false) deleteSchemas: Boolean) {
    if (deleteSchemas) {
      kafkaSchemaRegistryResetService.reset()
    }

    LOGGER.info("Resetting ...")
    resetRestoreDbs()
    resetServices.forEach { it.reset() }

    if (environment.acceptsProfiles(Profiles.of("local"))) {
      userInitializationService.initialize()
    }

    LOGGER.info("Reset finished")
  }

  @PostMapping("/init")
  fun initialize() {
    userInitializationService.initialize()
  }

  private fun resetRestoreDbs() {
    if (restoreDbResettableServices != null) {
      LOGGER.info("Reset restore data stores ...")
      restoreDbResettableServices.forEach { it.reset() }
    } else {
      LOGGER.info("Reset of restore data stores is not activated.")
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ResetController::class.java)
  }
}
