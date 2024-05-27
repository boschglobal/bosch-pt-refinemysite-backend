/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource

import java.time.LocalDate

class TimeMetricsResource(var start: LocalDate, var end: LocalDate, var metrics: MetricsResource)
