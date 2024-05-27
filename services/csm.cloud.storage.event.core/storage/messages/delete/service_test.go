package delete

import (
	"csm.cloud.storage.event.core/config/properties"
	"github.com/stretchr/testify/assert"
	"testing"
)

func Test_NewDefaultDeleteMessageService_PanicsWhenMisconfigured(t *testing.T) {

	assert.Panics(t, func() {
		NewDefaultDeleteMessageService(properties.StorageProperties{})
	}, "Calling service without connection string configured should panic")
}
