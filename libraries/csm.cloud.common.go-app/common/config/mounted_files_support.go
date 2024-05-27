package config

import (
	"fmt"
	"github.com/rs/zerolog/log"
	"github.com/spf13/viper"
	"os"
	"strings"
)

/*
loadCsiMountedSecrets - Loads secret key/values that have been mounted via CSI driver to the containers
file system. The location is determined from KV_VOLUME_ENV_PATH which is expected as environment variable
if applicable.
*/
func loadCsiMountedSecrets() error {

	// Determine mount path
	mntPath := os.Getenv("KV_VOLUME_ENV_PATH")
	if mntPath == "" {
		log.Info().Msg("No KV_VOLUME_ENV_PATH env variable pointing to usually mounted configs found")
		return nil
	}

	// Scan directory for files to read
	files, err := os.ReadDir(mntPath)
	log.Info().Msg(fmt.Sprintf("Reading configuration secrets from '%s'", mntPath))
	if err != nil {
		log.Warn().Msg(fmt.Sprintf("Unable to read configuration secrets from '%s'", mntPath))
		return err
	}

	// Read secret files and register them as properties
	for _, file := range files {
		log.Debug().Msg("CSI mounted file found: " + file.Name())

		// Read only secret files (ignore directories and invalid files)
		if !(file.IsDir() || strings.HasPrefix(file.Name(), ".")) {

			// Read secret file
			secret, err := os.ReadFile(mntPath + "/" + file.Name())
			if err != nil {
				log.Error().Msg("Unable to read secret" + err.Error())
				return err
			}

			// Derive the property name from file name
			key := strings.ReplaceAll(strings.ToUpper(file.Name()), "-", "_")
			key = strings.ReplaceAll(key, "_", ".")
			log.Info().Msg(fmt.Sprintf("Setting env variable from CSI mount with key: %s", key))

			// Register the property in the configuration
			viper.Set(key, string(secret))
		}
	}
	return nil
}
