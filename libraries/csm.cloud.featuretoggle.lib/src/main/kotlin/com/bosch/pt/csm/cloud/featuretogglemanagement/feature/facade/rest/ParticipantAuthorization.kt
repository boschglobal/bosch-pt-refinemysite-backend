package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.facade.rest

interface ParticipantAuthorization {
  fun isParticipantOf(projectIdentifier: String): Boolean
}
