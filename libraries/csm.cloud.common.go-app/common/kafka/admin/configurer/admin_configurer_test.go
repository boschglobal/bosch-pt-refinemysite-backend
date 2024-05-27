package configurer

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestConfigureKafkaAdminClient(t *testing.T) {

	// prepare
	brokerProperties := properties.BrokerProperties{
		Api: properties.ApiCredentials{
			Secret: "apiSecret!",
			Key:    "apiKey",
		},
		Urls:             "my-broker-url",
		SaslMechanism:    "PLAIN",
		SecurityProtocol: "SASL_SSL",
	}

	// execute
	cut := ConfigureKafkaAdminClient(brokerProperties)

	// verify
	assert.IsType(t, &kafka.AdminClient{}, cut)
}

func TestConfigureKafkaAdminClient_PanicsWhenMisconfigured(t *testing.T) {

	// prepare
	brokerProperties := properties.BrokerProperties{
		Api: properties.ApiCredentials{
			Secret: "apiSecret!",
			Key:    "apiKey",
		},
		Urls:             "my-broker-url",
		SaslMechanism:    "BrokenMechanism",
		SecurityProtocol: "BrokenProtocol",
	}

	// execute and verify
	assert.Panics(t, func() {
		ConfigureKafkaAdminClient(brokerProperties)
	})
}

func TestCreateKafkaAdminClientConfigMap(t *testing.T) {

	// prepare
	brokerProperties := properties.BrokerProperties{
		Api: properties.ApiCredentials{
			Secret: "apiSecret!",
			Key:    "apiKey",
		},
		Urls:             "my-broker-url",
		SaslMechanism:    "PLAIN",
		SecurityProtocol: "SASL_SSL",
	}

	// execute
	cut := createAdminConfigMap(brokerProperties)

	// verify
	assert.Equal(t, "PLAIN", getConfigValue(cut, "sasl.mechanisms"))
	assert.Equal(t, "SASL_SSL", getConfigValue(cut, "security.protocol"))
	assert.Equal(t, "apiKey", getConfigValue(cut, "sasl.username"))
	assert.Equal(t, "apiSecret!", getConfigValue(cut, "sasl.password"))
	assert.Equal(t, "my-broker-url", getConfigValue(cut, "bootstrap.servers"))
}

func TestCreateKafkaAdminClientConfigMap_NonSecuredBrokers(t *testing.T) {

	// prepare
	brokerProperties := properties.BrokerProperties{
		Urls: "my-broker-url",
	}

	// execute
	cut := createAdminConfigMap(brokerProperties)

	// verify
	assert.Equal(t, nil, getConfigValue(cut, "sasl.mechanisms"))
	assert.Equal(t, nil, getConfigValue(cut, "security.protocol"))
	assert.Equal(t, nil, getConfigValue(cut, "sasl.username"))
	assert.Equal(t, nil, getConfigValue(cut, "sasl.password"))
	assert.Equal(t, "my-broker-url", getConfigValue(cut, "bootstrap.servers"))
}

func getConfigValue(configMap *kafka.ConfigMap, key string) kafka.ConfigValue {
	val, _ := configMap.Get(key, nil)
	return val
}
