package config

import (
	"github.com/go-playground/validator/v10"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"os"
	"testing"
)

func TestLoadConfiguration(t *testing.T) {

	// prepare
	viper.Reset()

	// execute
	testConfiguration, err := LoadConfiguration(&TestConfiguration{}, "../../")

	// verify
	assert.Nil(t, err)
	assert.Equal(t, "testValue1", testConfiguration.(*TestConfiguration).TestConfigOne)
}

func TestLoadConfiguration_FailWithNoApplicationYml(t *testing.T) {

	// prepare
	viper.Reset()

	// execute
	_, err := LoadConfiguration(&TestConfiguration{}, "../wrong/root/")

	// verify
	assert.IsType(t, viper.ConfigFileNotFoundError{}, err)
}

func TestLoadConfiguration_FromCsiMount(t *testing.T) {

	// prepare
	os.Setenv("KV_VOLUME_ENV_PATH", "../../resources/mnt/")
	viper.Reset()

	// execute
	testConfiguration, err := LoadConfiguration(&TestConfiguration{}, "../../")

	// verify
	assert.Nil(t, err)
	assert.Equal(t, "testValue1", testConfiguration.(*TestConfiguration).TestConfigOne)
	assert.Equal(t, "secret-value", testConfiguration.(*TestConfiguration).Secrets.Secret)
}

func TestLoadConfiguration_FromProfile(t *testing.T) {

	// prepare
	viper.Reset()
	os.Setenv("GO_PROFILES_ACTIVE", "test")

	// execute
	testConfiguration, err := LoadConfiguration(&TestConfiguration{}, "../../")

	// verify
	assert.Nil(t, err)
	assert.Equal(t, "testValue1FromTestProfile", testConfiguration.(*TestConfiguration).TestConfigOne)
}

func TestLoadConfiguration_FromCsiMountOverwritesFromProfile(t *testing.T) {

	// prepare
	os.Setenv("KV_VOLUME_ENV_PATH", "../../resources/mnt/")
	os.Setenv("GO_PROFILES_ACTIVE", "test")
	viper.Reset()

	// execute
	testConfiguration, err := LoadConfiguration(&TestConfiguration{}, "../../")

	// verify
	assert.Nil(t, err)
	assert.Equal(t, "testValue1FromTestProfile", testConfiguration.(*TestConfiguration).TestConfigOne)
	assert.Equal(t, "secret-value", testConfiguration.(*TestConfiguration).Secrets.Secret)
}

func TestLoadConfiguration_ErrorOnMissingRequiredConfig(t *testing.T) {

	// prepare
	viper.Reset()

	// execute
	_, err := LoadConfiguration(&TestConfigurationUnsatisfied{}, "../../")

	// verify
	assert.IsType(t, validator.ValidationErrors{}, err)
}

type TestConfigurationUnsatisfied struct {
	MissingRequiredConfig string `validate:"required"`
	Secrets               TestSecrets
}

type TestConfiguration struct {
	TestConfigOne string `validate:"required"`
	Secrets       TestSecrets
}

type TestSecrets struct {
	Secret string
}
