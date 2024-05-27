package config

import (
	"csm.cloud.image.scale/config/properties"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config"
	commonProperties "dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
)

type Configuration struct {
	HttpClient commonProperties.HttpClientProperties
	Kafka      properties.KafkaProperties
	Server     properties.ServerProperties
	Storage    StorageConfiguration
}

type StorageConfiguration struct {
	Quarantine properties.StorageProperties
	User       properties.StorageProperties
	Project    properties.StorageProperties
}

func NewConfiguration(configRoot ...string) Configuration {

	// Load configuration
	configuration, err := config.LoadConfiguration(Configuration{}, configRoot...)
	if err != nil {
		panic(app.NewFatalError("Loading configuration failed", err))
	}
	return configuration.(Configuration)
}
