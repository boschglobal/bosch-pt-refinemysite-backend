package rest

import (
	"github.com/gin-gonic/gin"
	"net/http"
)

func LivenessEndpoint(context *gin.Context) {
	context.JSON(http.StatusOK, http.NoBody)
}
