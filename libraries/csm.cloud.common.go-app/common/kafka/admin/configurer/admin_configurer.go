package configurer

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
	"fmt"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
)

func ConfigureKafkaAdminClient(brokerProperties properties.BrokerProperties) *kafka.AdminClient {
	configMap := createAdminConfigMap(brokerProperties)
	adminClient, err := kafka.NewAdminClient(configMap)

	if err != nil {
		panic(app.NewFatalError(fmt.Sprintf("Failed to create Kafka Admin Client, bootstrapping with %s",
			brokerProperties.Urls), err))
	}

	return adminClient
}

func createAdminConfigMap(brokerProperties properties.BrokerProperties) *kafka.ConfigMap {
	configMap := &kafka.ConfigMap{"bootstrap.servers": brokerProperties.Urls}
	if brokerProperties.SaslMechanism != "" {
		_ = configMap.SetKey("sasl.mechanisms", brokerProperties.SaslMechanism)
	}
	if brokerProperties.SecurityProtocol != "" {
		_ = configMap.SetKey("security.protocol", brokerProperties.SecurityProtocol)
	}
	if brokerProperties.Api.Key != "" {
		_ = configMap.SetKey("sasl.username", brokerProperties.Api.Key)
	}
	if brokerProperties.Api.Secret != "" {
		_ = configMap.SetKey("sasl.password", brokerProperties.Api.Secret)
	}
	return configMap
}
