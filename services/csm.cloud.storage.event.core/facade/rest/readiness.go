package rest

import (
	"github.com/gin-gonic/gin"
	"net/http"
)

func ReadinessEndpoint(context *gin.Context) {
	context.JSON(http.StatusOK, http.NoBody)
}
