package com.bosch.pt.csm.cloud.usermanagement.common.translation
// ---------------------------------
// WARNING: Generated by Gradle task - NOT TO BE EDITED
// ---------------------------------

object Key {

    /**
     * en
     */
    const val KEY = "Key"

    /**
     * Invalid file.
     */
    const val ATTACHMENT_VALIDATION_ERROR_IOERROR = "Attachment_ValidationError_IOError"

    /**
     * The attachment wasn't found.
     */
    const val ATTACHMENT_VALIDATION_ERROR_NOT_FOUND = "Attachment_ValidationError_NotFound"

    /**
     * Record can not be updated because the provided version is outdated.
     */
    const val COMMON_VALIDATION_ERROR_ENTITY_OUTDATED = "Common_ValidationError_EntityOutdated"

    /**
     * Change of record not possible as record was already updated or deleted by another user.
     */
    const val COMMON_VALIDATION_ERROR_OPTIMISTIC_LOCKING = "Common_ValidationError_OptimisticLocking"

    /**
     * Creation or modification of the record is not possible because specified data of record violates integrity constraints.
     */
    const val COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED = "Common_ValidationError_DataIntegrityViolated"

    /**
     * The suggest term is too long.
     */
    const val COMMON_VALIDATION_ERROR_SUGGEST_TERM_TOO_LONG = "Common_ValidationError_SuggestTermTooLong"

    /**
     * The user is already registered.
     */
    const val COMMON_VALIDATION_ERROR_USER_ALREADY_REGISTERED = "Common_ValidationError_UserAlreadyRegistered"

    /**
     * The user is not registered yet.
     */
    const val COMMON_VALIDATION_ERROR_USER_NOT_REGISTERED = "Common_ValidationError_UserNotRegistered"

    /**
     * Contains no default translation.
     */
    const val CRAFT_VALIDATION_ERROR_NO_DEFAULT_NAME = "Craft_ValidationError_NoDefaultName"

    /**
     * Discipline is already in use.
     */
    const val PROJECT_CRAFT_VALIDATION_ERROR_USED_NAME = "ProjectCraft_ValidationError_UsedName"

    /**
     * Invalid image.
     */
    const val IMAGE_VALIDATION_ERROR_INVALID_IMAGE = "Image_ValidationError_InvalidImage"

    /**
     * The specified image type is not supported.
     */
    const val IMAGE_VALIDATION_ERROR_UNSUPPORTED_IMAGE_TYPE = "Image_ValidationError_UnsupportedImageType"

    /**
     * Given Personal Access Token (PAT) cannot be found.
     */
    const val PAT_VALIDATION_ERROR_NOT_FOUND = "Pat_ValidationError_NotFound"

    /**
     * Given Personal Access Token (PAT) belongs to a different user.
     */
    const val PAT_VALIDATION_ERROR_USER_MISMATCH = "Pat_ValidationError_UserMismatch"

    /**
     * The given Personal Access Token (PAT) period validity is out of allowed bounds.
     */
    const val PAT_VALIDATION_ERROR_VALIDITY_OUT_OF_BOUNDS = "Pat_ValidationError_ValidityOutOfBounds"

    /**
     * Given task cannot be found.
     */
    const val TASK_VALIDATION_ERROR_NOT_FOUND = "Task_ValidationError_NotFound"

    /**
     * User with assigned company cannot be deleted.
     */
    const val USER_VALIDATION_ERROR_DELETION_NOT_POSSIBLE = "User_ValidationError_DeletionNotPossible"

    /**
     * Given user cannot be found.
     */
    const val USER_VALIDATION_ERROR_NOT_FOUND = "User_ValidationError_NotFound"

    /**
     * The requested profile picture could not be found.
     */
    const val USER_VALIDATION_ERROR_PROFILE_PICTURE_NOT_FOUND = "User_ValidationError_ProfilePicture_NotFound"

    /**
     * First name, last name and gender must be given.
     */
    const val USER_VALIDATION_ERROR_REGISTRATION_DATA_INVALID = "User_ValidationError_RegistrationDataInvalid"

    /**
     * Some of the selected disciplines were not found.
     */
    const val USER_VALIDATION_ERROR_ASSOCIATED_CRAFT_NOT_FOUND = "User_ValidationError_AssociatedCraftNotFound"

    /**
     * The registration could not be finished successfully, because the End User License Agreement (EULA) has not been accepted.
     */
    const val USER_VALIDATION_ERROR_EULA_NOT_ACCEPTED_REGISTRATION = "User_ValidationError_EulaNotAcceptedRegistration"

    /**
     * The user profile could not be updated, because the End User License Agreement (EULA) has not been accepted.
     */
    const val USER_VALIDATION_ERROR_EULA_NOT_ACCEPTED_UPDATE = "User_ValidationError_EulaNotAcceptedUpdate"

    /**
     * User cannot remove their own admin permission.
     */
    const val USER_VALIDATION_ERROR_USER_NOT_REMOVING_OWN_ADMIN_PERMISSION = "User_ValidationError_UserNotRemovingOwnAdminPermission"

    /**
     * User cannot lock themselves out.
     */
    const val USER_VALIDATION_ERROR_USER_NOT_LOCKING_THEMSELVES = "User_ValidationError_UserNotLockingThemselves"

    /**
     * System user must not be modified.
     */
    const val USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED = "User_ValidationError_SystemUserMustNotBeModified"

    /**
     * Given document cannot be found.
     */
    const val DOCUMENT_VALIDATION_ERROR_NOT_FOUND = "Document_ValidationError_NotFound"

    /**
     * Not a valid increment! A version with a more recent date of change already exists.
     */
    const val DOCUMENT_VALIDATION_ERROR_INVALID_INCREMENT = "Document_ValidationError_InvalidIncrement"

    /**
     * A document for specified type, country, language and client already exists.
     */
    const val DOCUMENT_VALIDATION_ERROR_DOCUMENT_ALREADY_EXISTS = "Document_ValidationError_DocumentAlreadyExists"

    /**
     * Only language code is supported.
     */
    const val DOCUMENT_VALIDATION_ERROR_ONLY_LANGUAGE_SUPPORTED = "Document_ValidationError_OnlyLanguageSupported"

    /**
     * At least one specified document does not apply to the user.
     */
    const val CONSENTS_VALIDATION_ERROR_DOCUMENT_DOES_NOT_APPLY_TO_USER = "Consents_ValidationError_DocumentDoesNotApplyToUser"

}
