package request_host_rewrite

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
	"fmt"
	"github.com/rs/zerolog/log"
	"net/http"
)

/*
RegisterRequestHostRewriter enhances HTTP DefaultClient with client-side URL rewriting that replaces
the host name used in the request as per the rewrite rules configured
*/
func RegisterRequestHostRewriter(properties properties.HttpClientProperties) {

	//Initialize host rewriting if rewrite rules are specified
	if len(properties.RequestHostRewrites) > 0 {
		log.Info().Msg("registering url host name rewriter")

		// Initialize HTTP client with rewrite properties
		c := http.DefaultClient
		c.Transport = &urlHostRewriter{
			roundTripper: http.DefaultTransport,
			rewriteRules: properties.RequestHostRewrites,
		}

		// Log rewrite rules
		for _, rule := range properties.RequestHostRewrites {
			log.Debug().Msg(fmt.Sprintf("registered request host rewrite from '%s' to '%s'", rule.From, rule.To))
		}
	} else {
		log.Debug().Msg("No request host rewrites configured. Rewriter will not be applied")
	}
}

type urlHostRewriter struct {
	roundTripper http.RoundTripper
	rewriteRules []properties.RewriteProperties
}

/*
rewriteRequestHostNames replaces localhost with storage-emulator host in the request
*/
func (this *urlHostRewriter) rewriteRequestHostNames(request *http.Request) *http.Request {

	log.Trace().Msg(fmt.Sprintf("Evaluating rewrite for '%s'", request.Host))

	for _, rule := range this.rewriteRules {
		if request.Host == rule.From {
			log.Trace().Msg(fmt.Sprintf("Rewriting '%s' to '%s'", request.Host, rule.To))
			request.Host = rule.To
			request.URL.Host = rule.To
		}
	}

	return request
}

/*
RoundTrip implements the round tripper interface enhancing the request with rewriteRequestHostNames
*/
func (this *urlHostRewriter) RoundTrip(originalRequest *http.Request) (*http.Response, error) {

	// Modify HTTP request (i.e. replace hosts according to specified rules)
	adjustedRequest := this.rewriteRequestHostNames(originalRequest)

	// Execute the modified request
	return this.roundTripper.RoundTrip(adjustedRequest)
}
