package config

import (
	"fmt"
	"github.com/pkg/errors"
	"github.com/rs/zerolog/log"
	"github.com/spf13/viper"
	"strings"
)

/*
loadApplicationYamlConfigs load configuration from yaml configurations.
*/
func loadApplicationYamlConfigs(config any, configRoot ...string) (any, error) {
	// Configure replacer to replace `.` characters in property / env-var names with `_`
	viper.SetEnvKeyReplacer(strings.NewReplacer(`.`, `_`))

	// Check possible property sources
	viper.AutomaticEnv()

	// Add resources/application.yml as configuration source
	viper.SetConfigName("application")
	if configRoot == nil || len(configRoot) < 1 {
		viper.AddConfigPath("resources")
	} else if len(configRoot) > 1 {
		return config, errors.New("Invalid parameterization with more than one configRoot")
	} else {
		viper.AddConfigPath(configRoot[0] + "resources")
	}

	viper.SetConfigType("yml")

	// Merge values from different sources (env-vars, property files)
	err := viper.MergeInConfig()
	if err != nil {
		return config, err
	}

	// Load profile specific yml files (if exist)
	activeProfiles := GetActiveProfiles()
	for _, activeProfile := range activeProfiles {
		loadConfigYamlForProfile(activeProfile)
	}

	// Return the configuration
	return config, nil
}

/*
loadConfigYamlForProfile - attempts to load a configuration yml application-<profile>.yml
*/
func loadConfigYamlForProfile(profile string) {

	// Specify the filename to load
	viper.SetConfigName("application-" + profile)

	// Load and merge with existing configuration
	err := viper.MergeInConfig()

	// Log potential errors.
	if err != nil {
		log.Debug().Msg(fmt.Sprintf("No application-%s.yml found. Ignoring.", profile))
	} else {
		log.Debug().Msg(fmt.Sprintf("Successfully merged configuration from application-%s.yml", profile))
	}
}
