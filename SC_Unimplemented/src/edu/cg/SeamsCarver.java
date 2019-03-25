package edu.cg;

import java.awt.*;
import java.awt.Image;

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
	protected BufferedImage greyScaleImage; // a grey image of our original image that helps us find the pixel energy
	protected int[][] newImageXAxisRemapperArray;//this 2d array will help us to see where the new coordinates for the image are going to be after "removing"a seam without creating a new image
	private int numOfRemovedSeams;//the counter to how many seams we have already removed that will be used in StartValvulatingCostMatrix & so on

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
		greyScaleImage = greyscale();


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

	//----------------------------- new x coordinate helper array ----------------------------------------//


	/**
	 * initializes the helper remmapping array that will let us remove seams without creating new images
	 */
	private int[][] initiateNewImageXAxisRemapper()
	{

		newImageXAxisRemapperArray=new int[this.outWidth][this.outHeight];
		for (int x=0;x<newImageXAxisRemapperArray.length;x++)
		{
			for(int y =0;y<newImageXAxisRemapperArray[0].length;y++)
			{
				newImageXAxisRemapperArray[x][y]=x;
			}
		}
		return newImageXAxisRemapperArray;
	}

	/**
	 * the method takes advantage of the newImageXAxisRemapper array to get where the x should hae been in the original photo
	 * returns the X coordinate that should be in the after seam carving photo (after removing seams without creating new images)
	 */
	private int getCorrectXPositionForPixel(int x,int y)
	{
		return newImageXAxisRemapperArray[x][y];
	}


	//todo: should this be changed, as it is , the last columns stay the same since we dont really care about them

	/**
	 * the method takes the coordination of a cell
	 * and moves all the cells to its right 1 cell to the left in the  newImagexAxisRemapperArray
	 * @param startShiftingColumnsFromX
	 */
	private void shiftRowIndecesLeft(int startShiftingColumnsFromX,int y)
	{
		//goes from the starting position in the row , to the end of the "current" photo weidth after removing numOfRemovedSeams
		for(int i =startShiftingColumnsFromX;i<(inWidth-numOfRemovedSeams);i++)
		{
			newImageXAxisRemapperArray[i][y]=newImageXAxisRemapperArray[i+1][y];//todo: might have a problem when  numOfRemovedSeams=0
		}
	}

	///pcb:  el 3am te3malo eno bedak tem3la el shift left , 3ashan ba3de bas t'7ales te3mal eno ila2e el minimal path w i2eem el pixelem ele 3ala yameno
	//w ba3den bedak etla2e el ma7alat le lazem etbadel feha el x coordinate bel coordinate ta3et el helper matrix


	//---------------------------------------------cost matrix0-----------------------
	private long[][] startCalculatingCostMatrix()
	{

		greyScaleImage= greyscale(); // this variable is mainly for finding the pixel energy
		newImageXAxisRemapperArray = initiateNewImageXAxisRemapper();
		costMatrix = new long[greyScaleImage.getWidth()][greyScaleImage.getHeight()];//TODO: make sure its the right size



		//fills the first row with pixel energy
		for (int x=0;x<greyScaleImage.getWidth();x++) //TODO: change this because in buffered image we shuold start from 0 and not from 1???
		{
			costMatrix[x][0]=getPixelEnergy(x,0); //todo: make sure the coordinates are correct
		}

		//passses over the rest of the matrix , calculating each specifics cells/pixels cost
		for (int y = 1; y<greyScaleImage.getHeight();y++)
		{
			for(int x=0;x<greyScaleImage.getWidth();x++)//TODO: change this because in buffered image we shuold start from 0 and not from 1???
			{
				 calculateCostMatrixElement(x,y);
			}
		}

		return costMatrix;
	}


	/**
	 *this method is called from startCalculatingCostMatrix and calculates each specific elements in the cost matrix
	 */
	private void calculateCostMatrixElement(int x, int y)
	{

		//todo: mekre ketsoon , make sure this works
		if(x==0)//if we are on the first column , thus cant have an upper left corener
		{

			costMatrix[x][y]=getPixelEnergy(x, y) + Math.min(costMatrix[x][y-1],costMatrix[x+1][y-1]);

		}
		//todo: mekre ketsoon , make sure this works
		else if(x==greyScaleImage.getWidth())//if we are on the last column   //TODO: make sure this is right and we dont need .getwidth-1
		{
			costMatrix[x][y]=getPixelEnergy(x, y) + Math.min(costMatrix[x-1][y-1],costMatrix[x][y-1]);
		}
		else {
			//adds the new value to the matrix acording to the difinition in the homeowork pdf
			costMatrix[x][y]=getPixelEnergy(x, y) +
					minimumOfThree(
							costMatrix[x - 1][ y - 1]+ClForCostMatrix(greyScaleImage,x,y),
							costMatrix[x][y-1]+CvForCostMatrix(greyScaleImage,x,y),
							costMatrix[x+1][y - 1]+CrForCostMatrix(greyScaleImage,x,y)) ;
		}
	}

	//returns the minimum number between 3 long numbers
	private long minimumOfThree(long a,long b,long c)
	{
		return Math.min(a,Math.min(b,c));
	}


	/**
	 * gives you the pixel energy , as it is described in the recetation #2  slide 6
	 * calculates the "contrast"between the desired pixel and its neighbours
	 * gives a high pixel energy if the pixel is masked (which means we dont want it to be touched)
	 */
	public long getPixelEnergy (int x, int y) {

		long e1, e2, e3;

		if (y < inWidth - 1) {
			e1 = (long) ((new Color (greyScaleImage.getRGB(x, y)).getBlue()) -
					(new Color (greyScaleImage.getRGB(x, y + 1)).getBlue()));
		} else {
			e1 = (long) ((new Color (greyScaleImage.getRGB(x, y)).getBlue()) -
					(new Color (greyScaleImage.getRGB(x, y - 1)).getBlue()));
		}

		if (x < inHeight - 1) {
			e2 = (long) ((new Color (greyScaleImage.getRGB(x, y)).getBlue()) -
					(new Color (greyScaleImage.getRGB(x + 1, y)).getBlue()));
		} else {
			e2 = (long) ((new Color (greyScaleImage.getRGB(x, y)).getBlue()) -
					(new Color (greyScaleImage.getRGB(x - 1, y)).getBlue()));
		}

		if ( imageMask[x][y] ) {
			e3 = Integer.MAX_VALUE;
		} else {
			e3 = 0;
		}

		long pixelEnergy = e1 + e2 + e3;

		return pixelEnergy;
	}


	//TODO: remember there will be errors if you call this method from the first  column
	//todo: not sure f this is the correct implementation to begin with

	// named by its definition : returns the valued difference between (x,y)'s new neighbours  that were created depending on how the seam was removed  ,
	private	long ClForCostMatrix(BufferedImage greyscaleImage,int x,int y)
	{

		return Math.abs((int) ((new Color (greyscaleImage.getRGB(x+1, y)).getBlue())) - (int) ((new Color (greyscaleImage.getRGB(x-1, y)).getBlue()))) +Math.abs((int) ((new Color (greyscaleImage.getRGB(x-1, y)).getBlue()))- (int) ((new Color (greyscaleImage.getRGB(x, y-1)).getBlue())));

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


	//todo: writing the x axis ramapper methods first
	private void backTrackingForBestSeam()
	{
		//goal: to trace back according to the instruction in the recitation
		//to use the shiftRowIndecesLeft    to delete from the x axis remapper array instead of the image itself

		long lowestValueInCostMatrixForRowI;
		int yValueOfLowestInCM = -1;

		for(int i = inHeight-1; i >=0 ; i--) {

			lowestValueInCostMatrixForRowI = Long.MAX_VALUE;
			//find the lowest value in the row
			for(int j = 0; j < inWidth; j++) {
				if(costMatrix[i][j] < lowestValueInCostMatrixForRowI){
					lowestValueInCostMatrixForRowI = costMatrix[i][j];
					yValueOfLowestInCM = j;
				}
			}
			//shift all values after yValueOfLowestInCM
			for(int k = yValueOfLowestInCM; k < newImageXAxisRemapperArray.length; k++) {
				newImageXAxisRemapperArray[i][k] = newImageXAxisRemapperArray[i][k+1]; //shift values left
			}
		}

	}



}
