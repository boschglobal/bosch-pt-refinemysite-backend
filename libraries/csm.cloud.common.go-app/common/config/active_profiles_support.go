package config

import (
	"github.com/spf13/viper"
	"strings"
)

/*
GetActiveProfiles - Obtains list of active profiles from GO_PROFILES_ACTIVE config.
*/
func GetActiveProfiles() []string {
	if viper.IsSet("GO_PROFILES_ACTIVE") {

		// Get the profiles string from the environment variable and split it
		activeProfiles := viper.GetString("GO_PROFILES_ACTIVE")
		profilesRaw := strings.Split(activeProfiles, ",")

		// Collect non-empty profile strings
		var profiles []string
		for _, profile := range profilesRaw {
			trimmedProfile := strings.TrimSpace(profile)
			if trimmedProfile != "" {
				profiles = append(profiles, trimmedProfile)
			}
		}

		return profiles
	} else {
		var result []string
		return result
	}
}

/*
IsProfileActive checks if a profile is currently active.
*/
func IsProfileActive(profile string) bool {
	return contains(GetActiveProfiles(), profile)
}

/*
contains checks if a string is part of a given slice.
*/
func contains(s []string, str string) bool {
	for _, v := range s {
		if v == str {
			return true
		}
	}

	return false
}
