package config

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/go-playground/validator/v10"
	"github.com/rs/zerolog/log"
	"github.com/spf13/viper"
)

/*
LoadConfiguration - populates the config object returning it fully configured.
The following configuration is considered in that order:
- resources/application.yml
- resources/application-<activeProfile>.yml (multiple profiles can be considered)
- Secret or config values from CSI mounted volume
- Environment variables
*/
func LoadConfiguration(config any, configRoot ...string) (any, error) {

	config, err := initializeViperFromExistingConfig(config)
	if err != nil {
		return config, err
	}

	// Load properties from configuration files that have been mounted i.e. by CSI driver
	err = loadCsiMountedSecrets()
	if err != nil {
		log.Debug().Msg("Unable to read secrets from CSI-mounted volumes")
	}

	// Load explicitly defined set of env-variables
	err = loadEnvVariables()
	if err != nil {
		log.Debug().Msg("Unable to read env variables")
	}

	// Load properties from yml files
	config, err = loadApplicationYamlConfigs(config, configRoot...)
	if err != nil {
		return config, err
	}

	// Deserialize all properties from the various property sources
	err = viper.Unmarshal(&config)
	if err != nil {
		return config, err
	}

	// Validate configuration
	validate := validator.New()
	if err := validate.Struct(config); err != nil {
		log.Error().Msg(fmt.Sprintf("Missing required configuration %v", err))
		return config, err
	}
	return config, err
}

/*
initializeViperFromExistingConfig initializes viper with a given configuration.
It "registers" properties to prevent that viper complains if they are missing in one or the other
property source (e.g. in yml files from CSI mounted device or env-variables).
*/
func initializeViperFromExistingConfig(config any) (any, error) {

	// Serialize configuration as JSON
	allConfigKeys, err := json.Marshal(config)
	if err != nil {
		return config, err
	}

	// Initialize viper with JSON serialized properties
	viper.SetConfigType("JsonEncoding")
	err = viper.ReadConfig(bytes.NewReader(allConfigKeys))

	return config, err
}
