package request_host_rewrite

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
	"github.com/stretchr/testify/assert"
	"net/http"
	"testing"
)

func TestRegisterRequestHostRewriter(t *testing.T) {

	// prepare
	rewriteConfig := properties.HttpClientProperties{
		RequestHostRewrites: []properties.RewriteProperties{
			{
				From: "micky-mouse.bosch-refinemysite.com:9999",
				To:   "donald-duck.bosch-refinemysite.com:1234",
			},
		},
	}

	RegisterRequestHostRewriter(rewriteConfig)

	// execute
	_, err := http.DefaultClient.Get("noprot://micky-mouse.bosch-refinemysite.com:9999")

	//verify
	assert.Equal(t, "Get \"noprot://donald-duck.bosch-refinemysite.com:1234\": unsupported protocol scheme \"noprot\"", err.Error())
}
