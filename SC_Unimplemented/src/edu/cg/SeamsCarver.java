package edu.cg;

import java.awt.*;
//import java.awt.font.FontRenderContext;
//import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.Buffer;


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

	protected long[][] costMatrix;// a cost matrix that contains the path to be taken to find a desired seam
	protected BufferedImage greyScaleImage; // a grey image of our original image that helps us find the pixel energy
	protected int[][] xAxisRemapperArray;//this 2d array will help us to see where the new coordinates for the image are going to be after "removing"a seam without creating a new image
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


	//currentlyWorkingOn

	/**
	 * this is the starting method in the reduction process
	 * creates the x axis mapper array , that will hold the data to the final image and helps us along the process
	 * for each seam , we calculate the cost matrix  , the find which seam to remove
	 * after removing all the desired seams we will render the picture and return it
	 *
	 * @return the reduced image
	 */
	private BufferedImage reduceImageWidth() {
		xAxisRemapperArray = initiateXAxisRemapper();
		int reduceImageBy = (inWidth - outWidth);

		//removing one seam at a time
		for (numOfRemovedSeams = 0; numOfRemovedSeams < reduceImageBy; numOfRemovedSeams++) {
			//todo: make sure this is the way to do things
			backTrackingForBestSeam(startCalculatingCostMatrix());
		}


		//todo: should we add the transpose & redo the reduction process in this method???
		return renderImageFromXAxisRemapper();
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


	//---------------------------------------------cost matrix0-----------------------
	private long[][] startCalculatingCostMatrix() {

		greyScaleImage = greyscale(); // this variable is mainly for finding the pixel energy
		costMatrix = new long[inWidth-numOfRemovedSeams][inHeight];

		int x;
		//fills the first row with pixel energy & takes in concidiration the "already removed" pixels
		for ( x= 0; x < costMatrix.length; x++) //TODO: change this because in buffered image we shuold start from 0 and not from 1???  ***this todo applies to everything in this method****
		{
			costMatrix[x][0] = getPixelEnergy(x, 0);
		}

		//passses over the rest of the matrix , calculating each specifics cells/pixels cost
		for (int y = 1; y < greyScaleImage.getHeight(); y++)
		{
			for (x = 0; x < costMatrix.length; x++)
			{
				calculateCostMatrixElement(x, y);
			}
		}


		//after finishing the cost array , we start tracing back


		return costMatrix;
	}


	/**
	 * this method is called from startCalculatingCostMatrix and calculates each specific elements in the cost matrix
	 */
	private void calculateCostMatrixElement(int x, int y) {
		//this method , should not be called from the first row from the cost matrix
        long isNegative;//todo: remove this, its for checking

		//todo: mekre ketsoon , make sure this works
		if (x == 0)//if we are on the first column , thus cant have an upper left corener
		{

			costMatrix[x][y] = getPixelEnergy(x, y) + Math.min(costMatrix[x][y - 1], costMatrix[x + 1][y - 1]);

		}
		//todo: mekre ketsoon , make sure this works
		else if (x == costMatrix.length-1)//if we are on the last column   //TODO: make sure this is right and we dont need .getwidth-1
		{
			costMatrix[x][y] = getPixelEnergy(x, y) + Math.min(costMatrix[x - 1][y - 1], costMatrix[x][y - 1]);
		}
		else {
			//adds the new value to the matrix acording to the difinition in the homeowork pdf
			costMatrix[x][y] = getPixelEnergy(x, y) +
					minimumOfThree(
							costMatrix[x - 1][y - 1] + ClForCostMatrix(greyScaleImage, x, y),
							costMatrix[x][y - 1] + CvForCostMatrix(greyScaleImage, x, y),
							costMatrix[x + 1][y - 1] + CrForCostMatrix(greyScaleImage, x, y));
		}

        isNegative=costMatrix[x][y];
		isNegative=isNegative;

	}

	//returns the minimum number between 3 long numbers
	private long minimumOfThree(long a, long b, long c) {
		return Math.min(a, Math.min(b, c));
	}


	/**
	 * gives you the pixel energy , as it is described in the recetation #2  slide 6
	 * parameters: x,y    are the absolute x,y relative to the cost matrix
	 * calculates the "contrast" between the desired pixels neighbours  by remapping the positioning of the absolute x,y to the relative x,y in the original image after carving
	 * gives a high pixel energy if the pixel is masked (which means we dont want it to be touched)
	 */
	public long getPixelEnergy(int x, int y) {

		long e1, e2, e3;

		int remappedX=xAxisRemapper(x,y);

		//energy in columns
		if (remappedX < inWidth - 1)//as long as we are not in the last column (remember we need to remap the current x we have in the newly resized image, and check its coordinates in the original image)
		{
			e1 = (long) ((new Color(greyScaleImage.getRGB(remappedX, y)).getBlue()) -
					(new Color(greyScaleImage.getRGB(xAxisRemapper(x+1,y), y)).getBlue()));
		} else {
			e1 = (long) ((new Color(greyScaleImage.getRGB(remappedX, y)).getBlue()) -
					(new Color(greyScaleImage.getRGB(xAxisRemapper(x-1,y), y)).getBlue()));
		}


		//energy in rows
		if (y < inHeight - 1)
		{
			e2 = Math.abs( ((new Color(greyScaleImage.getRGB(remappedX, y)).getBlue()) -
					(new Color(greyScaleImage.getRGB(remappedX, y+1)).getBlue())));
		} else {
			e2 = Math.abs(((new Color(greyScaleImage.getRGB(remappedX, y)).getBlue()) -
					(new Color(greyScaleImage.getRGB(remappedX, y-1)).getBlue())));
		}

		/*
			On principle, pixelEnergy = e1 + e2 + e3.
			However, if we add a positive number to Integer.MAX_VALUE, we will receive a negative number.
			Therefore we will not add e1 and e2 to the calculation in cases where e3 = Integer.MAX_VALUE
		*/
		if (imageMask[y][remappedX]) {
			//remember in the array they submitted the rows and columns are different than yours
		    e3 = Integer.MAX_VALUE;
		} else {
			e3 = 0;
		}

		long pixelEnergy = e1 + e2 + e3;

		return pixelEnergy;
	}


	//TODO: remember there will be errors if you call this method from the first  column
	//todo: not sure f this is the correct implementation to begin with

	// named by its definition : returns the valued difference between original images (x,y)'s new neighbours  that were created depending on how the seam was removed  ,
	private long ClForCostMatrix(BufferedImage greyscaleImage, int x, int y) {

		return Math.abs((int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x+1,y), y)).getBlue())) - (int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x-1,y), y)).getBlue()))) + Math.abs((int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x-1,y), y)).getBlue())) - (int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x,y-1), y - 1)).getBlue())));

	}

	// named by its definition : returns the valued difference between original images (x,y)'s new neighbours  that were created depending on how the seam was removed  ,
	private long CvForCostMatrix(BufferedImage greyscaleImage, int x, int y) {

		return Math.abs((int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x+1,y), y)).getBlue())) - (int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x-1,y), y)).getBlue())));

	}

	// named by its definition : returns the valued difference between original images (x,y)'s new neighbours  that were created depending on how the seam was removed  ,
	private long CrForCostMatrix(BufferedImage greyscaleImage, int x, int y) {

		return Math.abs((int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x+1,y), y)).getBlue())) - (int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x-1,y), y)).getBlue()))) + Math.abs((int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x,y-1),y-1)).getBlue())) - (int) ((new Color(greyscaleImage.getRGB(xAxisRemapper(x+1,y), y)).getBlue())));

	}


	//------------------------------------back tracking---------------------------------------

	private void backTrackingForBestSeam(long[][] costMatrix) {
		int x = getBackTrackingStartingPositionColumn(costMatrix);//the x is currently the starting position for the back tracking
		int y = costMatrix[0].length - 1;// the y is the last row

		for (; y > 0; y--)//todo: review this part , check mekre ketson
		{
			shiftRowIndecesLeft(x, y);//shifts the columns in the helping array , starting the shift from the x position of the y row

			//gets x position for the row above it
			x = getBackTrackingPreviousRowXPositionForShiftRowLeft(x, y, costMatrix);
		}


		//goal: to trace back according to the instruction in the recitation
		//to use the shiftRowIndecesLeft    to delete from the x axis remapper array instead of the image itself

	}

	/**
	 * gives you the first cell where we should start back tracking from
	 * this will be the cell from the lowers row
	 */
	private int getBackTrackingStartingPositionColumn(long[][] costMatrix) {
		long min = Long.MAX_VALUE;
		int minPosition = 0;


		//you dont need to check the commplete last row in the cost matrix, you need you only need to check the length of the current picture
		//and the current picture might be already shortened , and is shorter than the original
		for (int x = 0; x < costMatrix.length - numOfRemovedSeams; x++) {
			if (costMatrix[x][costMatrix[0].length - 1] < min)//todo: make sure that the -1 is actually correct
			{
				min = costMatrix[x][costMatrix[0].length - 1];
				minPosition = x;
			}
		}

		return minPosition;
	}


	/**
	 * this method is called in each step going upwards in the cost matrix in backTrackingForBestSeam method
	 * it will return the x that we need to path through in the higher row while going upwards
	 * we will use the x position to shift left all the cells that are right to it in the xAxisRemapperArray
	 *
	 * @return the column of which we should shift left in the upper row ///todo: change this explnation
	 */
	private int getBackTrackingPreviousRowXPositionForShiftRowLeft(int x, int currentRowY, long[][] costMatrix) {
		int previousRowX;
		long currentCostMatrixCell = costMatrix[x][currentRowY];

		if ( //middle lane
				currentCostMatrixCell == getPixelEnergy(x, currentRowY) + costMatrix[x][currentRowY - 1] +
						CvForCostMatrix(greyScaleImage, x, currentRowY)) {
			previousRowX = x;
		} else if (currentCostMatrixCell == getPixelEnergy(x, currentRowY) + costMatrix[x - 1][currentRowY - 1] + ClForCostMatrix(greyScaleImage, x, currentRowY)) {
			previousRowX = x - 1;
		} else {
			previousRowX = x + 1;
		}

		return previousRowX;


	}


	//----------------------------- new x coordinate helper array ----------------------------------------//


	/**
	 * initializes the helper remmapping array that will let us remove seams without creating new images
	 */
	private int[][] initiateXAxisRemapper() {

		xAxisRemapperArray = new int[this.inWidth][this.inHeight];
		for (int x = 0; x < xAxisRemapperArray.length; x++) {
			for (int y = 0; y < xAxisRemapperArray[0].length; y++) {
				xAxisRemapperArray[x][y] = x;
			}
		}
		return xAxisRemapperArray;
	}

	/**xAxisRemapper
	 * the method takes advantage of the XAxisRemapper array to get where the x should hae been in the original photo
	 * returns the X coordinate that should be in the after seam carving photo (after removing seams without creating new images)
	 */
	private int xAxisRemapper(int x, int y) {
		return xAxisRemapperArray[x][y];
	}


	//todo: should this be changed, as it is , the last columns stay the same since we dont really care about them

	/**
	 * the method takes the coordination of a cell
	 * and moves all the cells to its right 1 cell to the left in the  xAxisRemapperArray
	 *
	 * @param startShiftingColumnsFromX
	 */
	private void shiftRowIndecesLeft(int startShiftingColumnsFromX, int y) {
		//goes from the starting position in the row , to the end of the "current" photo weidth after removing numOfRemovedSeams
		for (int i = startShiftingColumnsFromX; i < (inWidth - (numOfRemovedSeams + 1)); i++)//todo: should the -1 be there???
		{
			xAxisRemapperArray[i][y] = xAxisRemapperArray[i + 1][y];//todo: might have a problem when  numOfRemovedSeams=0
		}


		//todo: should this be changed, as it is , the last columns stay the same since we dont really care about them

	}


//------------------------------ image rendering------------------

	/**
	 * uses the X axis Mapper array , and the original photo , to render and return our new shrinked image
	 *
	 * @return our desired final buffered image
	 */
	private BufferedImage renderImageFromXAxisRemapper() {
		BufferedImage output = new BufferedImage(outWidth, outHeight, workingImageType);

		for (int y = 0; y < outHeight; y++) {
			for (int x = 0; x < outWidth; x++) {
				output.setRGB(x, y, workingImage.getRGB(xAxisRemapperArray[x][y], y));
				//each pixel in the new image, will take the color of the desired new pixel
				// the new desired pixel in the output image in position x, y       will be a pixel in row y in the original image
				//				while taking the pixel in column that is written in the x axis mapper array //TODO: rewrite this note
			}
		}
		return output;
	}

}
/*
	private void backTrackingForBestSeam2()
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
			for(int k = yValueOfLowestInCM; k < xAxisRemapperArray.length; k++) {
				xAxisRemapperArray[i][k] = xAxisRemapperArray[i][k+1]; //shift values left
			}
		}

	}
*/