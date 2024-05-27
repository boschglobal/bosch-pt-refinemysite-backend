package storage

import (
	"bytes"
	"context"
	"csm.cloud.image.scale/config/properties"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"fmt"
	"github.com/Azure/azure-sdk-for-go/sdk/azcore/policy"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azblob"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azblob/blob"
	"github.com/rs/zerolog/log"
	"net/http"
	"strings"
	"time"
)

type BlobStorageClient struct {
	client        *azblob.Client
	containerName string
}

func NewBlobStorageClient(properties properties.StorageProperties) BlobStorageClient {
	// Register DefaultClient as transport for AzureBlob client so that request interceptors apply
	clientOptions := &azblob.ClientOptions{
		ClientOptions: policy.ClientOptions{
			Transport: http.DefaultClient,
		},
	}

	// Instantiate the blob-client
	client, err := azblob.NewClientFromConnectionString(properties.ConnectionString, clientOptions)
	if err != nil {
		panic(app.NewFatalError("Unable to create client from connection string", err))
	}

	return BlobStorageClient{
		client:        client,
		containerName: properties.ContainerName,
	}
}

func (b *BlobStorageClient) DownloadBlob(path string, fileName string) (*Blob, error) {
	ctx, cancelFn := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancelFn()

	// Determine blob name
	blobName := strings.TrimPrefix(fmt.Sprintf("%s/%s", path, fileName), "/")

	// Download blob as stream (downloading as buffer doesn't seem to work)
	response, err := b.client.DownloadStream(ctx, b.containerName, blobName, nil)
	if err != nil {
		return nil, err
	}
	readCloser := response.Body
	// Close the stream
	defer func() {
		err = readCloser.Close()
		if err != nil {
			log.Warn().Msg(fmt.Sprintf("Error closing download stream %s", err.Error()))
		}
	}()

	// Read stream into byte buffer
	byteBuffer := new(bytes.Buffer)
	_, err = byteBuffer.ReadFrom(readCloser)
	if err != nil {
		return nil, err
	}

	// Convert metadata keys to lowercase
	metadata := make(map[string]*string)
	for entry := range response.Metadata {
		metadata[strings.ToLower(entry)] = response.Metadata[entry]
	}

	// Return byte buffer as byte array
	return &Blob{
		Buffer:      byteBuffer.Bytes(),
		ContentType: response.ContentType,
		Metadata:    metadata,
	}, nil
}

type Blob struct {
	Buffer      []byte
	ContentType *string
	Metadata    map[string]*string
}

func (b *BlobStorageClient) UploadBlob(path string, fileName string, buffer *[]byte, metadata map[string]*string, contentType string) error {
	ctx, cancelFn := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancelFn()

	// Determine blob name
	blobName := strings.TrimPrefix(fmt.Sprintf("%s/%s", path, fileName), "/")

	// Determine content disposition header
	originalFileName := metadata["filename"]
	if originalFileName == nil {
		originalFileName = &fileName
	}
	contentDisposition := fmt.Sprintf("attachment; filename=\"%s\"", *originalFileName)

	// Upload buffer
	options := azblob.UploadBufferOptions{
		HTTPHeaders: &blob.HTTPHeaders{BlobContentType: &contentType, BlobContentDisposition: &contentDisposition},
		Metadata:    metadata,
	}
	_, err := b.client.UploadBuffer(ctx, b.containerName, blobName, *buffer, &options)
	return err
}

func (b *BlobStorageClient) DeleteBlob(path string, fileName string) error {
	ctx, cancelFn := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancelFn()

	// Determine blob name
	blobName := strings.TrimPrefix(fmt.Sprintf("%s/%s", path, fileName), "/")

	// Delete blob
	_, err := b.client.DeleteBlob(ctx, b.containerName, blobName, nil)
	return err
}
