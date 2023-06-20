/*
 * Mars Simulation Project
 * CannedMarsMap.java
 * @date 2023-04-29
 * @author Greg Whelan
 */
package org.mars_sim.msp.ui.swing.tool.map;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;

import org.mars_sim.mapdata.MapData;
import org.mars_sim.mapdata.MapMetaData;
import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Msg;

/**
 * The CannedMarsMap class reads in data from files in the map_data jar file in
 * order to generate a map image.
 */
@SuppressWarnings("serial")
public class CannedMarsMap extends JComponent implements Map {

	/** default logger. */
	private static final Logger logger = Logger.getLogger(CannedMarsMap.class.getName());

	// Data members
	private boolean mapImageDone = false;
	
	private transient Image mapImage = null;
	
	private transient MapData mapData;
	
	private JComponent displayArea = null;

//	private Coordinates centerCoords = null;
	
	/**
	 * Constructor.
	 * 
	 * @param displayArea the component display area.
	 * @param type The type name
	 * @param mapData     the map data.
	 */
	protected CannedMarsMap(JComponent displayArea, MapData mapData) {
		this.mapData = mapData;
		this.displayArea = displayArea;
	}

	/** 
	 * Gets meta details of the underlying map.
	 */
	@Override
	public MapMetaData getType() {
		return mapData.getMetaData();
	}

	/**
	 * Creates a map image for a given center location.
	 * 
	 * @param center 	The center location of the map display.
	 * @param scale 	The map scale
	 * @return the map image.
	 */
	private Image createMapImage(Coordinates center, double scale) {
		return mapData.getMapImage(center.getPhi(), center.getTheta(), MapPanel.MAP_BOX_WIDTH, MapPanel.MAP_BOX_HEIGHT, scale);
	}
	
	/**
	 * Creates a 2D map at a given center point.
	 * 
	 * @param newCenter the new center location
	 */
	public void drawMap(Coordinates newCenter, double scale) {
//		if (scale != getScale()) {
//			|| centerCoords == null
//			|| !newCenter.equals(centerCoords)) {
			
			// FUTURE: need to find ways to reduce calling drawMap when only vehicle markers are being update 
		
			mapImage = createMapImage(newCenter, scale);
			
			if (displayArea == null) {
				logger.severe("displayArea is null.");
			}
			
			MediaTracker mt = new MediaTracker(displayArea);
			if (mapImage == null) {
				logger.severe("mapImage is null.");
			}
			mt.addImage(mapImage, 0);
			try {
				mt.waitForID(0);
				// Indicate that image is complete
				mapImageDone = true;
			} catch (InterruptedException e) {
				logger.log(Level.SEVERE, Msg.getString("CannedMarsMap.log.mediaTrackerInterrupted") + e); //$NON-NLS-1$
				// Restore interrupted state
			    Thread.currentThread().interrupt();
			}

//			centerCoords = newCenter;
			// Prepare and buffer the map
			bufferMap();
//		}
	}
	
	/**
	 * Draws a 2D map form the buffer.
	 */
	public void bufferMap() {
		paintDoubleBuffer();
		repaint();
	}

	/*
	 * Uses double buffering to draws into its own graphics object dbg before
	 * calling paintComponent().
	 */
	public void paintDoubleBuffer() {
		if (mapImage == null) {
			mapImage = createImage(MapPanel.MAP_BOX_WIDTH, MapPanel.MAP_BOX_HEIGHT);
			if (mapImage != null) {
				Graphics dbg = mapImage.getGraphics();
				Graphics2D g2d = (Graphics2D) dbg;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				
				Image image = getMapImage();
				if (image != null && mapImageDone)
					g2d.drawImage(image, 0, 0, this);		
			}
		}
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		if (mapImage != null)
			g2d.drawImage(mapImage, 0, 0, this);
		
		// Get rid of the copy
		g2d.dispose();
	}
	
	/**
	 * Checks if a requested map is complete.
	 * 
	 * @return true if requested map is complete
	 */
	@Override
	public boolean isImageDone() {
		return mapImageDone;
	}

	/**
	 * Gets the constructed map image.
	 * 
	 * @return constructed map image
	 */
	@Override
	public Image getMapImage() {
		return mapImage;
	}

	/**
	 * Gets the scale of the Mars surface map.
	 * 
	 * @return
	 */
	@Override
	public double getScale() {
		return mapData.getScale();
	}

	/**
	 * Sets the scale of the Mars surface map.
	 * 
	 * @return
	 */
	public void setMapScale(double value) {
		mapData.setMapScale(value);
	}
	   
	/**
	 * Gets the height of this map in pixels.
	 * 
	 * @return
	 */
	@Override
    public int getPixelHeight() {
		return mapData.getHeight();
	}

	/**
	 * Gets the width of this map in pixels.
	 * 
	 * @return
	 */
	@Override
    public int getPixelWidth() {
		return mapData.getWidth();
	}
	
	/**
	 * Prepares map panel for deletion.
	 */
	public void destroy() {
		mapImage = null;
		mapData = null;	
		displayArea = null;
//		centerCoords = null;
	}
}
