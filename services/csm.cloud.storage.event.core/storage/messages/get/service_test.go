package get

import (
	"csm.cloud.storage.event.core/config/properties"
	"github.com/stretchr/testify/assert"
	"testing"
)

func Test_NewDefaultGetMessageService_PanicsWhenMisconfigured(t *testing.T) {

	assert.Panics(t, func() {
		NewDefaultGetMessageService(properties.StorageProperties{})
	}, "Calling service without connection string configured should panic")
}

func Test_NewDefaultGetBlobInfoService_PanicsWhenMisconfigured(t *testing.T) {

	assert.Panics(t, func() {
		NewDefaultGetBlobInfoService(properties.StorageProperties{})
	}, "Connection to AzureStorage (blob) failed")
}
