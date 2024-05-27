package rest

import (
	"csm.cloud.storage.event.core/config/properties"
	"github.com/stretchr/testify/assert"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestInitRouter_Liveness(t *testing.T) {

	// prepare
	testRouter, err := initRouter()
	assert.Nil(t, err, "Router should initialize without error")
	recorder := httptest.NewRecorder()
	request, _ := http.NewRequest("GET", "/health/liveness", nil)

	// execute
	testRouter.ServeHTTP(recorder, request)
	status := recorder.Code

	// verify
	assert.Equal(t, http.StatusOK, status, "Liveness endpoint httpStatus should be 200")
	body := recorder.Body.String()
	assert.Equal(t, "{}", body, "Liveness endpoint should have an empty body")
}

func TestInitRouter_Readiness(t *testing.T) {

	// prepare
	testRouter, err := initRouter()
	assert.Nil(t, err, "Router should initialize without error")
	recorder := httptest.NewRecorder()
	request, _ := http.NewRequest("GET", "/health/readiness", nil)

	// execute
	testRouter.ServeHTTP(recorder, request)
	status := recorder.Code

	// verify
	assert.Equal(t, http.StatusOK, status, "Readiness endpoint httpStatus should be 200")
	body := recorder.Body.String()
	assert.Equal(t, "{}", body, "Readiness endpoint should have an empty body")
}

func TestWebServerRunner_RunFailsWhenPortIsInUse(t *testing.T) {

	runner := NewWebServerRunner(properties.ServerProperties{
		Port: 9999,
	})
	// start first web server runner on port 9999
	go runner.Run()

	// allow the other runner a second to spin up
	time.Sleep(time.Second * 1)

	// expect panic on the second web server runner on port 9999
	assert.Panics(t, func() {
		runner.Run()
	})
}
