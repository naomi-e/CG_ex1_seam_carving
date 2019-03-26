package edu.cg;

import java.awt.*;
//import java.awt.font.FontRenderContext;
//import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*; //for debugging

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
	private int[][] greyScaleArray;
	protected int[][] backTrackingArray;
	protected boolean[][] helpingMask; //important , remember the helping mask is transpose to the actual mask matrix
	protected long[][] pixelEnergyArray;





	///---===================================================== for debugging and checking
	protected long[][] transposedArray;
	private long[][] transposeArray(long[][] toTranspose)
	{
		transposedArray=new long[toTranspose[0].length][toTranspose.length];

		for(int y=0;y<toTranspose[0].length;y++)
		{
			for(int x=0;x<toTranspose.length;x++)
			{
				transposedArray[y][x]=toTranspose[x][y];
			}
		}
		return  transposedArray;
	}

	public static void usingFileWriter(String fileContent) throws IOException
	{

		FileWriter fileWriter = new FileWriter("c:/trial/costMatrix.txt");
		fileWriter.write(fileContent);
		fileWriter.close();
	}

	private void writeCostMatixToFile() throws IOException {
		String data="";
		for(int y=0;y<costMatrix[0].length;y++)
		{
			for(int x=0;x<costMatrix.length;x++)
			{
				data+="["+costMatrix[x][y]+"] ";
			}
			data+= System.lineSeparator();
		}


		usingFileWriter(data);
	}








	//------------------------------------------------ initializers ---------------------

	private int[][] getGreyScaleArray(BufferedImage greyScaleImage)
	{
		int[][] greyScaleArray = new int[inWidth][inHeight];
		for(int y=0;y<inHeight;y++)
		{
			for(int x=0;x<inWidth;x++)
			{
				greyScaleArray[x][y]=(int)(new Color(greyScaleImage.getRGB(x,y)).getBlue());
			}
		}
		return greyScaleArray;
	}
	private boolean[][] getHelpingMask()
	{
		boolean[][] helping = new boolean[inWidth][inHeight];
		for(int y=0;y<inHeight;y++)
		{
			for(int x=0;x<inWidth;x++)
			{
				helping[x][y]=imageMask[y][x];
			}
		}
		return helping;
	}

	private long[][] getPixelEnergyArray()
	{
		long[][] energyArray= new long[inWidth-numOfRemovedSeams][inHeight-numOfRemovedSeams];

		for(int y = 0; y<energyArray[0].length;y++)
		{
			for(int x=0;x<energyArray.length;x++)
			{
				energyArray[x][y]=getPixelEnergy(x,y);
			}
		}
		return energyArray;
	}


	//-----------------------------------------------------------------------------------

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
		greyScaleArray=getGreyScaleArray(greyScaleImage);
		xAxisRemapperArray = initiateXAxisRemapper();
		helpingMask=getHelpingMask();//this is the mask that gets shifted to the left the removal of each seam

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
		//throw new UnimplementedMethodException("getMaskAfterSeamCarving");

		boolean[][] bb=new boolean[outHeight][outWidth];
		return bb;
	}


	//---------------------------------------------cost matrix0-----------------------
	private long[][] startCalculatingCostMatrix() {
		logger.log("started Calculating Cost Matrix For Seam: "+(numOfRemovedSeams+1));




		costMatrix = new long[inWidth-numOfRemovedSeams][inHeight];
		backTrackingArray=new int[inWidth-numOfRemovedSeams][inHeight];
		pixelEnergyArray=getPixelEnergyArray();

		int x;
		//fills the first row with pixel energy & takes in concidiration the "already removed" pixels
		//for (x = 0; x < costMatrix.length; x++) //TODO: change this because in buffered image we shuold start from 0 and not from 1???  ***this todo applies to everything in this method****
		//{
		//	//costMatrix[x][0] = getPixelEnergy(x, 0);
		//	costMatrix[x][y] = pixelEnergyArray[x][y];
		//}



	/*	for(int y = 0 ;y<costMatrix[0].length;y++) {
			for (x = 0; x < costMatrix.length; x++) //TODO: change this because in buffered image we shuold start from 0 and not from 1???  ***this todo applies to everything in this method****
			{

				costMatrix[x][y] = pixelEnergyArray[x][y];
			}
		}
*/
		//passses over the rest of the matrix , calculating each specifics cells/pixels cost
		for (int y = 0; y < costMatrix[0].length; y++)
		{
			for (x = 0; x < costMatrix.length; x++)
			{
				calculateCostMatrixElement(x, y);
			}
		}


		//after finishing the cost array , we start tracing back

		//transposeArray(costMatrix);


	/*

		try {
			writeCostMatixToFile();
		}
		catch (Exception e)
		{}


		*/

		return costMatrix;
	}


	/**
	 * this method is called from startCalculatingCostMatrix and calculates each specific elements in the cost matrix
	 */
	private void calculateCostMatrixElement(int x, int y) {


		//costMatrix[x][y] = getPixelEnergy(x, y);
		//instead of going to the cost matrix each element at a time , i initilied the cost matrix to the pixel energy from the start calculate cost matrix

		//this method , should not be called from the first row from the cost matrix

		costMatrix[x][y]=getPixelEnergy(x,y);

		if(y>0) {
			long cL, cV, cR;


			//todo: mekre ketsoon , make sure this works
			if (x == 0)//if we are on the first column , thus cant have an upper left corener
			{
				cV = costMatrix[x][y - 1] + CvForCostMatrix(x, y);
				cR = costMatrix[x + 1][y - 1] + CrForCostMatrix(x, y);
				cL = Integer.MAX_VALUE;
			}
			//todo: mekre ketsoon , make sure this works
			else if (x == costMatrix.length - 1)//if we are on the last column   //TODO: make sure this is right and we dont need .getwidth-1
			{
				cL = costMatrix[x - 1][y - 1] + ClForCostMatrix(x, y);
				cV = costMatrix[x][y - 1] + CvForCostMatrix(x, y);
				cR = Integer.MAX_VALUE;
			} else {
				//adds the new value to the matrix acording to the difinition in the homeowork pdf

				cL = costMatrix[x - 1][y - 1] + ClForCostMatrix(x, y);
				cV = costMatrix[x][y - 1] + CvForCostMatrix(x, y);
				cR = costMatrix[x + 1][y - 1] + CrForCostMatrix(x, y);
			}

			costMatrix[x][y] += minimumOfThree(cL, cV, cR);
			backTrackingArray[x][y] = backTrackingArrayFiller(costMatrix[x][y], x, cL, cR);
		}

	}

	/**
	 * called from calculate cost matrix element, takes the element of the cost matrix, its position , and its neighbour , and returns to which x it should go when it back trakcs
	 * @return
	 */
	private int backTrackingArrayFiller(long costMatrixElement,int xPosition,long cL,long cR)
	{
		if (xPosition > 0 && costMatrixElement == cL) {
			return xPosition - 1;
		} else if (xPosition < (inWidth-numOfRemovedSeams) && costMatrixElement == cR) {
			return xPosition + 1;
		}
		return xPosition;



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

		//int remappedX=xAxisRemapper(x,y);



		//energy in columns

		/*if (remappedX < inWidth - 1)//as long as we are not in the last column (remember we need to remap the current x we have in the newly resized image, and check its coordinates in the original image)
		{
			e1 = Math.abs( ((new Color(greyScaleImage.getRGB(remappedX, y)).getBlue()) -
					(new Color(greyScaleImage.getRGB(xAxisRemapper(x+1,y), y)).getBlue())));
		} else {
			e1 = Math.abs( ((new Color(greyScaleImage.getRGB(remappedX, y)).getBlue()) -
					(new Color(greyScaleImage.getRGB(xAxisRemapper(x-1,y), y)).getBlue())));
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
		*/

		//energy in columns
		if (x < greyScaleArray.length - 1)//as long as we are not in the last column (remember we need to remap the current x we have in the newly resized image, and check its coordinates in the original image)
		{
			//e1= Math.abs( greyScaleArray[remappedX][y] - greyScaleArray[xAxisRemapper(x+1,y)][y]);
			e1= Math.abs( greyScaleArray[x][y] - greyScaleArray[x+1][y]);


		} else {
			//e1=Math.abs( greyScaleArray[x][y] - greyScaleArray[xAxisRemapper(x-1,y)][y]);
			e1=Math.abs( greyScaleArray[x][y] - greyScaleArray[x-1][y]);
		}


		//energy in rows
		if (y < greyScaleArray[0].length - 1)
		{
			//e2= Math.abs( greyScaleArray[remappedX][y] - greyScaleArray[remappedX][y+1]);
			e2= Math.abs( greyScaleArray[x][y] - greyScaleArray[x][y+1]);
		} else {
			//e2=Math.abs( greyScaleArray[remappedX][y] - greyScaleArray[remappedX][y-1]);
			e2=Math.abs( greyScaleArray[x][y] - greyScaleArray[x][y-1]);
		}

		/*
			On principle, pixelEnergy = e1 + e2 + e3.
			However, if we add a positive number to Integer.MAX_VALUE, we will receive a negative number.
			Therefore we will not add e1 and e2 to the calculation in cases where e3 = Integer.MAX_VALUE
		*/


		//if (imageMask[y][remappedX]) {
		if (helpingMask[x][y]) {
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
	private long ClForCostMatrix( int x, int y) {

		long cl = 0;

		if (x <= 0 || x >= (inWidth - (numOfRemovedSeams+1)))//todo: make sure this is correct ,    checks if its a border
		{//todo make sure the x==0 is correct
			cl = 255; //if its a border then count it as important data
		} else // if in the middle of the matrix
		{
			//cl = Math.abs(greyScaleArray[xAxisRemapper(x + 1, y)][y] - greyScaleArray[xAxisRemapper(x - 1,y)][y]); // get the neighbours
			cl = Math.abs(greyScaleArray[x + 1][y] - greyScaleArray[x - 1][y]); // get the neighbours

		}

		if (x > 0) //if we are not on the first column
		{
			//cl+= Math.abs( greyScaleArray[xAxisRemapper(x-1,y)][y] -  greyScaleArray[xAxisRemapper(x,y-1)][y-1]);
			cl+= Math.abs( greyScaleArray[x-1][y] -  greyScaleArray[x][y-1]);


		}


		return cl;
	}

	// named by its definition : returns the valued difference between original images (x,y)'s new neighbours  that were created depending on how the seam was removed  ,
	private long CvForCostMatrix( int x, int y) {

		long cv=0;
		if (x <= 0 ||x >= (inWidth - (numOfRemovedSeams+1)))//todo: make sure this is correct ,    checks if its a border
		{//todo make sure the x==0 is correct
			cv = 255; //if its a border then count it as important data
		}
		else // if in the middle of the matrix
		{
			//cv=Math.abs(greyScaleArray[xAxisRemapper(x + 1, y)][y] - greyScaleArray[xAxisRemapper(x - 1,y)][y]);
			cv=Math.abs(greyScaleArray[x + 1][y] - greyScaleArray[x - 1][y]);

		}
		return cv;
		//;;
	}

	// named by its definition : returns the valued difference between original images (x,y)'s new neighbours  that were created depending on how the seam was removed  ,
	private long CrForCostMatrix(int x, int y) {

		long cr;
		if (x <= 0 ||x >= (inWidth - (numOfRemovedSeams+1)))//todo: make sure this is correct ,    checks if its a border
		{//todo make sure the x==0 is correct
			cr = 255; //if its a border then count it as important data
		}
		else // if in the middle of the matrix
		{
			//cr=Math.abs(greyScaleArray[xAxisRemapper(x + 1, y)][y] - greyScaleArray[xAxisRemapper(x - 1,y)][y]);
			cr=Math.abs(greyScaleArray[x + 1][y] - greyScaleArray[x - 1][y]);

		}

		if(x<(inWidth-(numOfRemovedSeams+1)))
		{
		//	cr += Math.abs(greyScaleArray[xAxisRemapper(x,y-1)][y-1] - greyScaleArray[xAxisRemapper(x+1,y)][y]);
			cr += Math.abs(greyScaleArray[y-1][y-1] - greyScaleArray[x+1][y]);

		}
		return cr;


	}


	//------------------------------------back tracking---------------------------------------

	private void backTrackingForBestSeam(long[][] costMatrix) {


		this.logger.log("started Backtracking For Seam"+(numOfRemovedSeams+1));
		//transposeArray(costMatrix);
		int x = getBackTrackingStartingPositionColumn(costMatrix);//the x is currently the starting position for the back tracking
		int y = costMatrix[0].length - 1;// the y is the last row

		for (; y > 0; y--)//todo: review this part , check mekre ketson
		{
			shiftRowIndecesLeft(x, y);//shifts the columns in the helping array , starting the shift from the x position of the y row

			//gets x position for the row above it
			x = getBackTrackedColumnFor(x, y, costMatrix);
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
		for (int x = 0; x < costMatrix.length; x++) {
			if (costMatrix[x][costMatrix[0].length - 1] < min)//todo: make sure that the -1 is actually correct
			{
				min = costMatrix[x][costMatrix[0].length - 1];
				minPosition = x;
			}
		}

		this.logger.log("selected pixel in position "+minPosition+"to start back tracking for seam:"+(numOfRemovedSeams+1));

		return minPosition;
	}


	/**
	 * this method is called in each step going upwards in the cost matrix in backTrackingForBestSeam method
	 * it will return the x that we need to path through in the higher row while going upwards
	 * we will use the x position to shift left all the cells that are right to it in the xAxisRemapperArray
	 *
	 * previously: getBackTrackingPreviousRowXPositionForShiftRowLeft
	 *
	 * @return the column of which we should shift left in the upper row ///todo: change this explnation
	 */
	private int getBackTrackedColumnFor(int x, int currentRowY, long[][] costMatrix) {
		return backTrackingArray[x][currentRowY];

		/*
		int previousRowX;
		long currentCostMatrixCell = costMatrix[x][currentRowY];


		//pcb: el mohskele he lama bakon b2a'7er column se33etha anu sh'7enem bedo i3mal

       // currentCostMatrixCell=

        //todo: this needs rechecking
        if(x==0)//if we are on the first column   then we have only 2 options to go upwards
        {
            if(currentCostMatrixCell == getPixelEnergy(x, currentRowY) + costMatrix[x][currentRowY - 1])
            {
                previousRowX = x;
            }
                else
            {
                previousRowX = x+1;
            }
        }
        //if we are on the last column  then w have only 2 options to go upwards
        else if (x >= costMatrix.length-1)
        {
            if(currentCostMatrixCell == getPixelEnergy(x, currentRowY) + costMatrix[x][currentRowY - 1])
            {
                previousRowX = x;
            }
            else
            {
                previousRowX = x-1;
            }
        }
        //if we are in the middle then we have 3 options , and we need to check the new neighbours that were created
        else
        {
            //look at the one above
            if(currentCostMatrixCell == getPixelEnergy(x, currentRowY) + costMatrix[x][currentRowY - 1] + CvForCostMatrix( x, currentRowY)) {
                previousRowX = x;
                //look at the left
            } else if (currentCostMatrixCell == getPixelEnergy(x, currentRowY) + costMatrix[x - 1][currentRowY - 1] + ClForCostMatrix(x, currentRowY)) {
                previousRowX = x - 1;
            } else {
                previousRowX = x + 1;
            }
        }

		return previousRowX;
	*/

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
			greyScaleArray[i][y]=greyScaleArray[i+1][y];
			helpingMask[i][y]=helpingMask[i+1][y];
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
