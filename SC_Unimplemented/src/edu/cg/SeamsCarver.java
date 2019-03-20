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


	protected  long[][] costMatrix;// a cost matrix that contains the path to be taken to find a desired seam
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


	public BufferedImage resize() {
		return resizeOp.resize();
	}

	private BufferedImage reduceImageWidth() {
		// TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("reduceImageWidth");
	}

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


	///takes a grey image, modifies the protected cost matrix and returns it,
	///
	private long[][] startCalculatingCostMatrix(BufferedImage greyScaleImage)
	{
		costMatrix = new long[greyScaleImage.getWidth()+1][greyScaleImage.getHeight()+1];//TODO: make sure its the right size

		//fills the first row with pixel energy
		for (int x=1;x<=greyScaleImage.getWidth();x++)
		{
			costMatrix[x][1]=getPixelEnergy(x,1);
		}

		//passses over the rest of the matrix , calculating each specifics cells/pixels cost
		for (int y = 1; y<=greyScaleImage.getHeight();y++)
		{
			for(int x=1;x<=greyScaleImage.getWidth();x++)
			{
				 calculateCostMatrixElement(greyScaleImage,x,y);
			}
		}


		return costMatrix;
	}


	//returns the minimum number between 3 long numbers
	private long minimumOfThree(long a,long b,long c)
	{
		return Math.min(a,Math.min(b,c));
	}

	//TODO: remember there will be errors if you call this method from the first  column
	//todo: not sure f this is the correct implementation to begin with

	// named by its definition : returns the valued difference between (x,y)'s new neighbours  that were created depending on how the seam was removed  ,
	private	long ClForCostMatrix(BufferedImage greyscaleImage,int x,int y)
	{

		return Math.abs((int) ((new Color (greyscaleImage.getRGB(x+1, y)).getBlue())) - (int) ((new Color (greyscaleImage.getRGB(x-1, y)).getBlue()))) +Math.abs((int) ((new Color (greyscaleImage.getRGB(x-1, y)).getBlue()))- (int) ((new Color (greyscaleImage.getRGB(x, y-1)).getBlue())))

	}
	// named by its definition : returns the valued difference between (x,y)'s new neighbours  that were created depending on how the seam was removed  ,
	private long CvForCostMatrix(BufferedImage greyscaleImage,int x , int y)
	{

			return Math.abs((int) ((new Color (greyscaleImage.getRGB(x+1, y)).getBlue())) - (int) ((new Color (greyscaleImage.getRGB(x-1, y)).getBlue()))) ;

	}
	// named by its definition : returns the valued difference between (x,y)'s new neighbours  that were created depending on how the seam was removed  ,
	private long CrForCostMatrix(BufferedImage greyscaleImage,int x , int y)
	{

		return Math.abs((int) ((new Color (greyscaleImage.getRGB(x+1, y)).getBlue())) - (int) ((new Color (greyscaleImage.getRGB(x-1, y)).getBlue()))) + Math.abs((int) ((new Color (greyscaleImage.getRGB(x, y-1)).getBlue())) - (int) ((new Color (greyscaleImage.getRGB(x+1, y)).getBlue())));

	}


	//changes the cost of this pixel , in the cost matrix
	private void calculateCostMatrixElement(BufferedImage greyscaleImage,int x, int y)
	{

		//if we are on the first column , thus cant have an upper left corener
		//todo: mekre ketsoon , make sure this works
		if(x==1)
		{

			costMatrix[x][y]=getPixelEnergy(x, y) + Math.min(costMatrix[x][y-1],costMatrix[x+1][y-1]);

		}
		//todo: mekre ketsoon , make sure this works
		else if(x==greyscaleImage.getWidth())
		{
			costMatrix[x][y]=getPixelEnergy(x, y) + Math.min(costMatrix[x-1][y-1],costMatrix[x][y-1]);

		}
		else {
			//adds the new value to the matrix acording to the difinition in the homeowork pdf
			costMatrix[x][y]=getPixelEnergy(x, y) + minimumOfThree(costMatrix[x - 1][ y - 1]+ClForCostMatrix(greyscaleImage,x,y),costMatrix[x][y-1]+CvForCostMatrix(greyscaleImage,x,y),costMatrix[x+1][y - 1]+CrForCostMatrix(greyscaleImage,x,y)) ;
		}
	}
}
