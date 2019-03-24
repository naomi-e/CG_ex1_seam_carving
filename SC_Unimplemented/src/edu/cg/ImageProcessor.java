package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {

	// MARK: fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;

	// MARK: constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights, int outWidth,
			int outHeight) {
		super(); // initializing for each loops...

		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}

	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights, workingImage.getWidth(), workingImage.getHeight());
	}

	// MARK: change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Preparing for hue changing...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r * c.getRed() / max;
			int green = g * c.getGreen() / max;
			int blue = b * c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Changing hue done!");

		return ans;
	}

	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}

	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}

	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}

	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}

	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}

	// A helper method that deep copies the current working image.
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		setForEachInputParameters();
		forEach((y, x) -> output.setRGB(x, y, workingImage.getRGB(x, y)));

		return output;
	}

	/**
	 * Takes a colored image and returns it in greyscale
	 * @return greyscale BufferedImage
	 */
	public BufferedImage greyscale() {
		logger.log("Preparing for greyscaling...");

		BufferedImage greyscaleImage = newEmptyInputSizedImage();

		// for each pixel, average the red, green and blue weights. set their weights to the average
		forEach((y, x) -> {
			//get the colors for pixel at x,y in image
			Color coloredPixel = new Color(workingImage.getRGB(x, y));
			int weightedColor = coloredPixel.getRed() * rgbWeights.redWeight +
								coloredPixel.getGreen() * rgbWeights.greenWeight +
								coloredPixel.getBlue() * rgbWeights.blueWeight;
			weightedColor /= (rgbWeights.redWeight + rgbWeights.greenWeight + rgbWeights.blueWeight);

			Color greyscalePixel = new Color(weightedColor, weightedColor, weightedColor);
			greyscaleImage.setRGB(x, y, greyscalePixel.getRGB());

		});

		//TODO: REMOVE THIS!

		//SeamsCarver
		//SeamsCarver.getPixelEnergy(0,0);

		return greyscaleImage;


	}


	/**
	 * scales the image acording to the new desired size which is the this.outwidth , this.outhight
	 * @return returns a buffered image with the new dimention
	 */
	public BufferedImage nearestNeighbor() {
		// firstly we calculate the new aspect ratio for both x,y axis
		//after that we will linearly map the old image into the new one

		BufferedImage outputBufferedImage=  new BufferedImage(this.outWidth, this.outHeight, workingImageType);

		double xAxisRatio = this.inWidth/(double)this.outWidth;
		double yAxisRatio = this.inHeight/(double)this.outHeight;




		//goes over each row then each column
		for(int y=0;y<outHeight;y++)//todo: make sure starting from 1 is correct
		{
			for(int x=0;x<outWidth;x++)//todo: make sure starting from 1 is correct5
			{
				//does a linear transformation from the new desired size to the original images pixel
				//in a way where the new pixel will get the color of the (old pixel position) * (new aspect ratio difference)
				outputBufferedImage.setRGB(x,y,this.workingImage.getRGB((int) (xAxisRatio*x) , (int)(yAxisRatio*y)));
				//since we are flooring the number we might get rough edges
			}
		}
		return	outputBufferedImage;
	}
}
