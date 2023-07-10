/*
 * Mars Simulation Project
 * GlobeMap.java
 * @date 2023-06-03
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.navigator;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.ImageObserver;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;

import org.mars.sim.mapdata.MapMetaData;
import org.mars.sim.mapdata.location.Coordinates;
import org.mars.sim.tools.Msg;
import org.mars.sim.tools.util.MoreMath;

/**
 * The GlobeMap class generates a Mars globe.
 */
public class GlobeMap {

	/** default logger. */
	private static final Logger logger = Logger.getLogger(GlobeMap.class.getName());

	// Constant data members
	/** Height of map source image (pixels). */
	private static final int MAP_H = NavigatorWindow.MAP_BOX_WIDTH;
	/** Width of map source image (pixels). */
	private static final int MAP_W = MAP_H * 2;
	private static final int HALF_MAP_HEIGHT = MAP_H / 2;
	
	private static final double PI_HALF = Math.PI / 2;
	private static final double PI_DOUBLE = Math.PI * 2;
	private static final double RHO = MAP_H / Math.PI;
	private static final double COL_ARRAY_MODIFIER = 1 / PI_DOUBLE;

	private int[] imageArray;

	// Data members
	/** Center position of globe. */
	private Coordinates centerCoords;
	/** point colors in variably-sized vectors. */
	@SuppressWarnings("unchecked")
	private Vector<Integer>[] sphereColor = new Vector[MAP_H];
	/** cylindrical map image. */
	private Image cylindricalMapImage;
	/** finished image of sphere with transparency. */
	private Image globeImage;
	/** true when image is done. */
	private boolean mapImageDone;
	/** parent display area. */
	private JComponent displayArea;
	
	private GlobeDisplay globeDisplay;

	/**
	 * Constructs a MarsMap object.
	 * 
	 * @param mapType   the type of globe
	 * @param displayArea the display component for the map
	 */
	public GlobeMap(GlobeDisplay globeDisplay, MapMetaData mapType, JComponent displayArea) {

		// Initialize Variables
		this.globeDisplay = globeDisplay;
		this.displayArea = displayArea;
		centerCoords = new Coordinates(PI_HALF, 0);

		cylindricalMapImage =  globeDisplay.getNavigatorWindow().getMapPanel().getMapData().getCylindricalMapImage();

		// Locate the image file which may be downloaded from a remote site
//		File imageFile = FileLocator.locateFile(mapType.getLoResFile());
//		try {
//			cylindricalMapImage = ImageIO.read(imageFile);
//		} catch (IOException e) {
//			logger.severe("Can't read image file ");
//		}
		// Prepare Sphere
		setupSphere();
	}

	/**
	 * Creates a Sphere Image at given center point.
	 * 
	 * @param newCenter new center location
	 */
	public synchronized void drawSphere(Coordinates newCenter) {
		// Adjust coordinates
		Coordinates adjNewCenter = new Coordinates(newCenter.getPhi(), newCenter.getTheta() + Math.PI);

		// If current center point equals new center point, don't recreate sphere
		if (centerCoords.equals(adjNewCenter)) {
			return;
		}

		// Initialize variables
		mapImageDone = false;

		centerCoords = adjNewCenter;

		double phi = centerCoords.getPhi();
		double theta = centerCoords.getTheta();
		double end_row = phi - PI_HALF;
		double start_row = end_row + Math.PI;
		double row_iterate;
		boolean north;

		// Determine if sphere should be created from north-south, or from south-north
		if (phi <= PI_HALF) {
			north = true;
			end_row = phi - PI_HALF;
			start_row = end_row + Math.PI;
			row_iterate = 0D - (Math.PI / (double) MAP_H);
		} else {
			north = false;
			start_row = phi - PI_HALF;
			end_row = start_row + Math.PI;
			row_iterate = (Math.PI / (double) MAP_H);
		}

		// More variable initializations
		double col_correction = -PI_HALF - theta;
		// double rho = map_height / Math.PI;
		double sin_offset = MoreMath.sin(phi + Math.PI);
		double cos_offset = MoreMath.cos(phi + Math.PI);

		// Create array to hold image
		imageArray = new int[MAP_H * MAP_H];

		// Go through each row of the sphere
		for (double row = start_row; (((north) && (row >= end_row))
				|| ((!north) && (row <= end_row))); row += row_iterate) {
			if (row < 0)
				continue;
			if (row >= Math.PI)
				continue;
			int array_y = (int) Math.round(((double) MAP_H * row) / Math.PI);
			if (array_y >= MAP_H)
				continue;

			// Determine circumference of this row
			int circum = sphereColor[array_y].size();
			double row_cos =  MoreMath.cos(row);

			// Determine visible boundary of row
			double col_boundry = Math.PI;
			if (phi <= PI_HALF) {
				if ((row >= PI_HALF *  MoreMath.cos(phi)) && (row < PI_HALF)) {
					col_boundry = PI_HALF * (1D + row_cos);
				} else if (row >= PI_HALF) {
					col_boundry = PI_HALF;
				}
			} else {
				if ((row <= PI_HALF *  MoreMath.cos(phi)) && (row > PI_HALF)) {
					col_boundry = PI_HALF * (1D - row_cos);
				} else if (row <= PI_HALF) {
					col_boundry = PI_HALF;
				}
			}
			if (phi == PI_HALF) {
				col_boundry = PI_HALF;
			}

			double col_iterate = Math.PI / (double) circum;

			// Error adjustment for theta center close to PI_half
			double error_correction = phi - PI_HALF;

			if (error_correction > 0D) {
				if (error_correction < row_iterate) {
					col_boundry = PI_HALF;
				}
			} else if (error_correction > 0D - row_iterate) {
				col_boundry = PI_HALF;
			}

			// Determine column starting and stopping points for row
			double start_col = theta - col_boundry;
			double end_col = theta + col_boundry;
			if (col_boundry == Math.PI)
				end_col -= col_iterate;

			double temp_buff_x = RHO * MoreMath.sin(row);
			double temp_buff_y1 = temp_buff_x * cos_offset;
			double temp_buff_y2 = RHO * row_cos * sin_offset;

			double col_array_modifier2 = COL_ARRAY_MODIFIER * circum;

			// Go through each column in row
			for (double col = start_col; col <= end_col; col += col_iterate) {
				int array_x = (int) (col_array_modifier2 * col);

				if (array_x < 0) {
					array_x += circum;
				} else if (array_x >= circum) {
					array_x -= circum;
				}

				double temp_col = col + col_correction;

				// Determine x and y position of point on image
				int buff_x = (int) Math.round(temp_buff_x *  MoreMath.cos(temp_col)) + HALF_MAP_HEIGHT;
				int buff_y = (int) Math.round((temp_buff_y1 * MoreMath.sin(temp_col)) + temp_buff_y2) + HALF_MAP_HEIGHT;

				// Put point in buffer array
				imageArray[buff_x + (MAP_H * buff_y)] = (int) sphereColor[array_y].elementAt(array_x);
				// buffer_array[buff_x + (map_height * buff_y)] = 0xFFFFFFFF; // if in gray
				// scale
			}
		}
		
		drawMap(newCenter);
	}
	
//	/**
//	 * NOTE: Do retain this method for future use
//	 * 
//	 * Creates a map image for a given center location.
//	 * 
//	 * @param center the center location of the map display.
//	 * @return the map image.
//	 */
//	private Image createMapImage(Coordinates center) {
//		return globeDisplay.getNavigatorWindow().getMapPanel().getMapData().getMapImage(center.getPhi(), center.getTheta(), MapPanel.MAP_BOX_WIDTH, MapPanel.MAP_BOX_HEIGHT);
//	}
	
	/**
	 * Creates a 2D map at a given center point.
	 * 
	 * @param newCenter the new center location
	 */
	public void drawMap(Coordinates newCenter) {	
		
		// Create image out of buffer array
		globeImage = displayArea
				.createImage(new MemoryImageSource(MAP_H, MAP_H, imageArray, 0, MAP_H));

//		globeImage = createMapImage(newCenter);
		
		if (displayArea == null) {
			logger.severe("displayArea is null.");
		}
		
		MediaTracker mt = new MediaTracker(displayArea);
		if (globeImage == null) {
			logger.severe("globeImage is null.");
		}
		mt.addImage(globeImage, 0);
		try {
			mt.waitForID(0);
			// Indicate that image is complete
			mapImageDone = true;
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, Msg.getString("MarsMap.log.mediaTrackerError", e.toString())); //$NON-NLS-1$
			// Restore interrupted state
		    Thread.currentThread().interrupt();
		}
	}

	/**
	 * Returns globe image.
	 * 
	 * @return globe image
	 */
	public Image getGlobeImage() {
		return globeImage;
	}

	/** 
	 * Sets up Points and Colors for Sphere. 
	 * */
	private void setupSphere() {

		// Initialize variables
		int row, col_num, map_col;
		double phi, theta;
		double circum, offset;
		double ih_d = (double) MAP_H;

		// Initialize color arrays
		int[] pixelsColorArray = new int[MAP_H * MAP_W];
		int[][] mapPixelsArray = new int[MAP_W][MAP_H];

		// Grab mars_surface image into pixels_color array using PixelGrabber
		// NOTE: Replace PixelGrabber with faster method
		
		// Scale cylindricalMapImage down to MAP_H * MAP_W
		PixelGrabber pixelGrabber = new PixelGrabber(cylindricalMapImage.getScaledInstance(MAP_W, MAP_H, 4), 0, 0, MAP_W, MAP_H, pixelsColorArray, 0, MAP_W);
		
		try {
			pixelGrabber.grabPixels();
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, Msg.getString("MarsMap.log.grabberError") + e); //$NON-NLS-1$
			// Restore interrupted state
		    Thread.currentThread().interrupt();
		}
		
		if ((pixelGrabber.status() & ImageObserver.ABORT) != 0)
			logger.info(Msg.getString("MarsMap.log.grabberError")); //$NON-NLS-1$

		// Transfer contents of 1-dimensional pixels_color into 2-dimensional map_pixels
		for (int x = 0; x < MAP_W; x++)
			for (int y = 0; y < MAP_H; y++)
				mapPixelsArray[x][y] = pixelsColorArray[x + (y * MAP_W)];
		
		// Initialize variables
		offset = PI_HALF / ih_d;

		// Go through each row and create Sphere_Color vector with it
		for (phi = offset; phi < Math.PI; phi += (Math.PI / ih_d)) {
			row = MoreMath.floor((float) ((phi / Math.PI) * ih_d));//(int) Math.floor((phi / Math.PI) * ih_d);
			circum = PI_DOUBLE * (RHO * MoreMath.sin(phi));
			col_num = (int) Math.round(circum);
			sphereColor[row] = new Vector<Integer>(col_num);

			// Fill vector with colors
			for (theta = 0; theta < PI_DOUBLE; theta += (PI_DOUBLE / circum)) {
				if (theta == 0) {
					map_col = 0;
				} else {
					map_col = MoreMath.floor((float)((theta / Math.PI) * ih_d));
				}

				sphereColor[row].addElement(mapPixelsArray[map_col][row]);
			}
		}
	}

	/**
	 * Determines if a requested sphere is complete.
	 * 
	 * @return true if image is done
	 */
	public boolean isImageDone() {
		return mapImageDone;
	}

	/**
	 * Prepares globe for deletion.
	 */
	public void destroy() {
		centerCoords = null;
		sphereColor = null;
		cylindricalMapImage = null;
		globeImage = null;
		displayArea = null;
		mapImageDone = true;
		imageArray = null;
	}
}
