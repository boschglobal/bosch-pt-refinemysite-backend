package config

import (
	"github.com/stretchr/testify/assert"
	"os"
	"testing"
)

func TestConfigurationFailsWithWrongConfigPath(t *testing.T) {

	// execute and verify
	assert.Panics(t, func() {
		NewConfiguration()
	})
}

func TestConfigurationSucceeds(t *testing.T) {

	// prepare
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// execute
	config := NewConfiguration("../")

	//verify
	assert.Equal(t, "quarantineuploads", config.Storage.QueueName)
}
