package rest

import (
	"csm.cloud.image.scale/config/properties"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog/log"
	"strconv"
)

type WebServerRunner struct {
	serverConfiguration properties.ServerProperties
}

func NewWebServerRunner(configuration properties.ServerProperties) WebServerRunner {
	return WebServerRunner{serverConfiguration: configuration}
}

func (this *WebServerRunner) Run() {

	// Initialize web-server
	router, err := initRouter()
	if err != nil {
		panic(app.NewFatalError("Router initialization failed", err))
	}

	// Start web-server
	port := strconv.Itoa(this.serverConfiguration.Port)
	log.Info().Msg("Web server listening on port " + port)
	err = router.Run(":" + port)
	if err != nil {
		panic(app.NewFatalError("Web server process failed", err))
	}
}

func initRouter() (*gin.Engine, error) {
	// Configure logging of route-functions
	gin.DebugPrintRouteFunc = func(httpMethod, absolutePath, handlerName string, nuHandlers int) {
		log.Debug().Msg(fmt.Sprintf("endpoint: %v %v %v", httpMethod, absolutePath, handlerName))
	}

	// Set release mode to hide debug messages
	gin.SetMode(gin.ReleaseMode)

	// Configure router
	router := gin.New()
	router.Use(gin.Recovery())
	err := router.SetTrustedProxies(nil)
	if err != nil {
		return nil, err
	}

	// Add routing for the health endpoints
	router.GET("/health/liveness", LivenessEndpoint)
	router.GET("/health/readiness", ReadinessEndpoint)

	// Return the router
	return router, nil
}
