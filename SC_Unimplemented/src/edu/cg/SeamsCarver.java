package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SeamsCarver extends ImageProcessor {

	// MARK: An inner interface for functional programming.
	@FunctionalInterface
	interface ResizeOperation {
		BufferedImage resize();
	}

	// MARK: Fields
	private int numOfSeams;
	private ResizeOperation resizeOp;
	boolean[][] imageMask;
	protected BufferedImage greyscaleImage;

	// TODO: Add some additional fields

	public SeamsCarver(Logger logger, BufferedImage workingImage, int outWidth, RGBWeights rgbWeights,
			boolean[][] imageMask) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, workingImage.getHeight());

		numOfSeams = Math.abs(outWidth - inWidth);
		this.imageMask = imageMask;
		if (inWidth < 2 | inHeight < 2)
			throw new RuntimeException("Can not apply seam carving: workingImage is too small");

		if (numOfSeams > inWidth / 2)
			throw new RuntimeException("Can not apply seam carving: too many seams...");

		// Setting resizeOp by with the appropriate method reference
		if (outWidth > inWidth)
			resizeOp = this::increaseImageWidth;
		else if (outWidth < inWidth)
			resizeOp = this::reduceImageWidth;
		else
			resizeOp = this::duplicateWorkingImage;

		// TODO: You may initialize your additional fields and apply some preliminary calculations.
		greyscaleImage = greyscale();


		this.logger.log("preliminary calculations were ended.");
	}


	public BufferedImage resize() { return resizeOp.resize(); }

	/**
	 * to reduce an image's width, we can remove the seams one by one - numOfSeams times
	 * @return a buffered image of width outWidth
	 */
	private BufferedImage reduceImageWidth() {
		// TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("reduceImageWidth");
	}

	/**
	 * to enlarge an image, we must first find all numOfSeams seams, and then remove them one by one
	 * @return buffered image of width outWidth
	 */
	private BufferedImage increaseImageWidth() {
		// TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("increaseImageWidth");
	}

	public BufferedImage showSeams(int seamColorRGB) {
		// TODO: Implement this method (bonus), remove the exception.
		throw new UnimplementedMethodException("showSeams");
	}

	public boolean[][] getMaskAfterSeamCarving() {
		// TODO: Implement this method, remove the exception.
		// This method should return the mask of the resize image after seam carving. Meaning,
		// after applying Seam Carving on the input image, getMaskAfterSeamCarving() will return
		// a mask, with the same dimensions as the resized image, where the mask values match the
		// original mask values for the corresponding pixels.
		// HINT:
		// Once you remove (replicate) the chosen seams from the input image, you need to also
		// remove (replicate) the matching entries from the mask as well.
		throw new UnimplementedMethodException("getMaskAfterSeamCarving");
	}

	//----------------------------- GRADIENT ----------------------------------------//


	public int getPixelEnergy (int x, int y) {

		int e1, e2;

		if (y < inWidth - 1) {
			e1 = (int) ((new Color (greyscaleImage.getRGB(x, y)).getBlue()) -
					(new Color (greyscaleImage.getRGB(x, y + 1)).getBlue()));
		} else {
			e1 = (int) ((new Color (greyscaleImage.getRGB(x, y)).getBlue()) -
					(new Color (greyscaleImage.getRGB(x, y - 1)).getBlue()));
		}

		if (x < inHeight - 1) {
			e2 = (int) ((new Color (greyscaleImage.getRGB(x, y)).getBlue()) -
					(new Color (greyscaleImage.getRGB(x + 1, y)).getBlue()));
		} else {
			e2 = (int) ((new Color (greyscaleImage.getRGB(x, y)).getBlue()) -
					(new Color (greyscaleImage.getRGB(x - 1, y)).getBlue()));
		}

		/*
			On principle, pixelEnergy = e1 + e2 + e3.
			However, if we add a positive number to Integer.MAX_VALUE, we will receive a negative number.
			Therefore we will not add e1 and e2 to the calculation in cases where e3 = Integer.MAX_VALUE
		*/
		int pixelEnergy;
		if ( imageMask[x][y] ) {
			pixelEnergy = Integer.MAX_VALUE;
		} else {
			pixelEnergy = e1 + e2;
		}
		return pixelEnergy;
	}

	//public BufferedImage forward
}
