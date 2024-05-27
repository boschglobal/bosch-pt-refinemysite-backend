package image

import (
	"github.com/davidbyttow/govips/v2/vips"
)

type ImageSizeProperties interface {
	GetHeight() int
	GetWidth() int
	GetInteresting() vips.Interesting
}

type DefaultImageSizeProperties struct {
	height      int
	width       int
	interesting vips.Interesting
}

var SmallImageSizeProperties = DefaultImageSizeProperties{
	height:      250,
	width:       250,
	interesting: vips.InterestingAttention,
}

var PreviewImageSizeProperties = DefaultImageSizeProperties{
	height:      1920,
	width:       1920,
	interesting: vips.InterestingNone,
}

func (d *DefaultImageSizeProperties) GetHeight() int {
	return d.height
}

func (d *DefaultImageSizeProperties) GetWidth() int {
	return d.width
}

func (d *DefaultImageSizeProperties) GetInteresting() vips.Interesting {
	return d.interesting
}

func ScaleImage(buffer *[]byte, sizeProperties ImageSizeProperties) (*[]byte, error) {
	image, err := vips.NewImageFromBuffer(*buffer)
	if err != nil {
		return nil, err
	}
	defer image.Close()

	_ = image.AutoRotate()

	// with an original image sized 768 * 1024 the following happens when target width and height are 150:
	// vips.InterestingNone => 112 × 150 (ratio preserved - width and height serve as maximum value respectively)
	// vips.InterestingAll => 150 x 200 (ratio preserved - width and height are used as minimum value at least when down-scaling)
	// vips.InterestingCentre => 150 x 150 (ratio preserved, cutting upper and lower parts of the image away to fit the desired width and height)
	bytes, err := resize(image, sizeProperties.GetWidth(), sizeProperties.GetHeight(), sizeProperties.GetInteresting())

	return bytes, err
}

func resize(image *vips.ImageRef, width int, height int, crop vips.Interesting) (*[]byte, error) {

	// do resize the image
	err := image.ThumbnailWithSize(width, height, crop, vips.SizeBoth)
	if err != nil {
		return nil, err
	}

	blob, _, err := image.ExportJpeg(&vips.JpegExportParams{
		StripMetadata: true,
		Quality:       90,
		// Generate interlaced (progressive) jpeg
		Interlace: true,
		// Compute optimal Huffman coding tables, shrinks jpegs
		OptimizeCoding: true,
		// Disable chrominance subsampling, improves quality
		SubsampleMode: vips.VipsForeignSubsampleOff,
	})
	if err != nil {
		return nil, err
	}

	return &blob, nil
}
