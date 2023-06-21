/*
 * Mars Simulation Project
 * RandomMineralMap.java
 * @date 2023-06-14
 * @author Scott Davis
 */

package org.mars_sim.msp.core.environment;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Direction;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.environment.MineralMapConfig.MineralType;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * A randomly generated mineral map of Mars.
 */
public class RandomMineralMap implements Serializable, MineralMap {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(RandomMineralMap.class.getName());

	private static final int W = 300;
	private static final int H = 150;
	
	private static final int NUM_REGIONS = 50;
	private static final int NUM_CONCONCENTRATIONS = 500;
	
	private static final float REGION_FACTOR = 5;
	private static final float NON_REGION_FACTOR = 100;
	
	// Topographical Region Strings
	private static final String CRATER_IMG = Msg.getString("RandomMineralMap.image.crater"); //$NON-NLS-1$
	private static final String VOLCANIC_IMG = Msg.getString("RandomMineralMap.image.volcanic"); //$NON-NLS-1$
	private static final String SEDIMENTARY_IMG = Msg.getString("RandomMineralMap.image.sedimentary"); //$NON-NLS-1$

	private static final String CRATER_REGION = "crater";
	private static final String VOLCANIC_REGION = "volcanic";
	private static final String SEDIMENTARY_REGION = "sedimentary";

	// Frequency Strings
	private static final String COMMON_FREQUENCY = "common";
	private static final String UNCOMMON_FREQUENCY = "uncommon";
	private static final String RARE_FREQUENCY = "rare";
	private static final String VERY_RARE_FREQUENCY = "very rare";

	// List of all mineral concentrations.
	private List<MineralConcentration> mineralConcentrations;
	
	private Map<Coordinates, Map<String, Double>> allMineralsByLocation;

	private static MineralMapConfig mineralMapConfig = SimulationConfig.instance().getMineralMapConfiguration();
	
	/**
	 * Constructor.
	 */
	RandomMineralMap() {
		mineralConcentrations = new ArrayList<>(NUM_CONCONCENTRATIONS);
		
		allMineralsByLocation = new HashMap<>();
		
		// Determine mineral concentrations.
		determineMineralConcentrations();
//		System.out.println("mineralConcentrations: " + mineralConcentrations.size()); // 2496 -> 278
	}

	/**
	 * Determines all mineral concentrations.
	 */
	private void determineMineralConcentrations() {
		// Load topographical regions.
		Set<Coordinates> craterRegionSet = getTopoRegionSet(CRATER_IMG);
		Set<Coordinates> volcanicRegionSet = getTopoRegionSet(VOLCANIC_IMG);
		Set<Coordinates> sedimentaryRegionSet = getTopoRegionSet(SEDIMENTARY_IMG);

		if (mineralMapConfig == null)
			mineralMapConfig = SimulationConfig.instance().getMineralMapConfiguration();
		
		List<MineralType> minerals = new ArrayList<>(mineralMapConfig.getMineralTypes());
		Collections.shuffle(minerals);
		int size = minerals.size(); 
		
		try {
			Iterator<MineralType> i = minerals.iterator();
			while (i.hasNext()) {
				MineralType mineralType = i.next();
				// For now start with a random concentration between 0 to 100
				int conc = RandomUtil.getRandomInt(100);
				
				// Create super set of topographical regions.
				Set<Coordinates> regionSet = new HashSet<>(NUM_REGIONS);
				Iterator<String> j = mineralType.getLocales().iterator();
				while (j.hasNext()) {
					String locale = j.next().trim();
					if (CRATER_REGION.equalsIgnoreCase(locale))
						regionSet.addAll(craterRegionSet);
					else if (VOLCANIC_REGION.equalsIgnoreCase(locale))
						regionSet.addAll(volcanicRegionSet);
					else if (SEDIMENTARY_REGION.equalsIgnoreCase(locale))
						regionSet.addAll(sedimentaryRegionSet);
				}
				Coordinates[] regionArray = regionSet.toArray(new Coordinates[regionSet.size()]);
				// Will have one region array for each of 10 types of minerals 
//				System.out.println("regionArray: " + regionArray.length); // between 850 and 3420 for each mineral
				
				if (regionArray.length > 0) {
					// Determine individual mineral concentrations.
					int concentrationNumber = Math.round((float) regionArray.length 
							/ REGION_FACTOR / getFrequencyModifier(mineralType.frequency));
					// Get the new remainingConc by multiplying it with concentrationNumber; 
					double remainingConc = conc * concentrationNumber;
					
					// Test the generation of concentrationNumber 
//					System.out.println("1. region concentrationNumber: " + concentrationNumber); // between 34 and 684 (now 6 -> 68) for diff minerals
					for (int x = 0; x < concentrationNumber; x++) {
						int regionLocationIndex = RandomUtil.getRandomInt(regionArray.length - 1);
						Coordinates regionLocation = regionArray[regionLocationIndex];
						Direction direction = new Direction(RandomUtil.getRandomDouble(Math.PI * 2D));
						double pixelRadius = (Coordinates.MARS_CIRCUMFERENCE / W) / 2D;
						double distance = RandomUtil.getRandomDouble(pixelRadius);
						Coordinates location = regionLocation.getNewLocation(direction, distance);
						double concentration = 0;
						
						if (remainingConc < 0)
							concentration = 0;
						else if (x != size - 1)
							concentration = Math.round(RandomUtil.getRandomDouble(conc *.75, conc * 1.25));
						else
							concentration = Math.round(remainingConc);
						
						if (concentration > 100)
							concentration = 100;
						if (concentration < 0) {
							concentration = 0;
						}
						
						remainingConc -= concentration;
						
//						System.out.println(mineralType + ": " + concentration); 
						
						mineralConcentrations.add(new MineralConcentration(location, concentration, mineralType.name));
					}
				}
				
				size++;
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error creating random mineral map.", e);
		}
	}

	/**
	 * Creates concentration at a specified location.
	 * 
	 * @param location
	 */
	public Map<String, Double> createConcentration(Coordinates location) {
		
		List<MineralConcentration> localMinerals = new ArrayList<>();
		
		// Load topographical regions.
		Set<Coordinates> craterRegionSet = getTopoRegionSet(CRATER_IMG);
		Set<Coordinates> volcanicRegionSet = getTopoRegionSet(VOLCANIC_IMG);
		Set<Coordinates> sedimentaryRegionSet = getTopoRegionSet(SEDIMENTARY_IMG);

		if (mineralMapConfig == null)
			mineralMapConfig = SimulationConfig.instance().getMineralMapConfiguration();
		
		List<MineralType> minerals = new ArrayList<>(mineralMapConfig.getMineralTypes());
		Collections.shuffle(minerals);
		int size = minerals.size(); 
	
		Iterator<MineralType> i = minerals.iterator();
		while (i.hasNext()) {
			MineralType mineralType = i.next();

			// For now start with a random concentration between 0 to 100
			int conc = RandomUtil.getRandomInt(30);

			int concentrationNumber = (int)Math.round(
					RandomUtil.getRandomDouble(NON_REGION_FACTOR *.25, NON_REGION_FACTOR * 1.25) 
					/ getFrequencyModifier(mineralType.frequency));
			
			// Get the new remainingConc by multiplying it with concentrationNumber; 
			double remainingConc = conc * concentrationNumber;
			
			// Test the generation of concentrationNumber 
			for (int x = 0; x < concentrationNumber; x++) {
				double concentration = 0;
				
				if (remainingConc < 0)
					concentration = 0;
				else if (x != size - 1)
					concentration = Math.round(RandomUtil.getRandomDouble(conc *.75, conc * 1.25));
				else
					concentration = Math.round(remainingConc);
				
				if (concentration > 100)
					concentration = 100;
				if (concentration < 0) {
					concentration = 0;
				}
				
				remainingConc -= concentration;
				
				System.out.println(mineralType + ": " + concentration); 
				
				localMinerals.add(new MineralConcentration(location, concentration, mineralType.name));
			}
		}
		
		mineralConcentrations.addAll(localMinerals);
		
		Map<String, Double> map = new HashMap<>();
		
		Iterator<MineralConcentration> k = localMinerals.iterator();
		while (k.hasNext()) {
			MineralConcentration mineralConcentration = k.next();
			double effect = getMineralConcentrationEffect(mineralConcentration, location);
			if (effect > 0D) {
	
				double totalConcentration = 0D;
				if (map.containsKey(mineralConcentration.getMineralType()))
					totalConcentration = map.get(mineralConcentration.getMineralType());
				totalConcentration += effect;
				if (totalConcentration > 100D)
					totalConcentration = 100D;
				map.put(mineralConcentration.getMineralType(), totalConcentration);
			}
		}
		
		allMineralsByLocation.put(location, map);
		
		return map;
	}
	
	/**
	 * Gets the dividend due to frequency of mineral type.
	 * 
	 * @param frequency the frequency ("common", "uncommon", "rare" or "very rare").
	 * @return frequency modifier.
	 */
	private float getFrequencyModifier(String frequency) {
		float result = 1F;
		if (COMMON_FREQUENCY.equalsIgnoreCase(frequency.trim()))
			result = 10; // 1F;
		else if (UNCOMMON_FREQUENCY.equalsIgnoreCase(frequency.trim()))
			result = 30; // 5F;
		else if (RARE_FREQUENCY.equalsIgnoreCase(frequency.trim()))
			result = 60; // 10F;
		else if (VERY_RARE_FREQUENCY.equalsIgnoreCase(frequency.trim()))
			result = 90; // 15F;
		return result;
	}

	/**
	 * Gets a set of location coordinates representing a topographical region.
	 * 
	 * @param imageMapName the topographical region map image.
	 * @return set of location coordinates.
	 */
	private Set<Coordinates> getTopoRegionSet(String imageMapName) {
		Set<Coordinates> result = new HashSet<>(3000);
//		[landrus, 26.11.09]: don't use the system classloader in a webstart env.
		URL imageMapURL = getClass().getResource("/images/" + imageMapName);
		ImageIcon mapIcon = new ImageIcon(imageMapURL);
		Image mapImage = mapIcon.getImage();

		int[] mapPixels = new int[W * H];
		PixelGrabber topoGrabber = new PixelGrabber(mapImage, 0, 0, W, H, mapPixels, 0, W);
		try {
			topoGrabber.grabPixels();
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "grabber error" + e);
			// Restore interrupted state
		    Thread.currentThread().interrupt();
		}
		if ((topoGrabber.status() & ImageObserver.ABORT) != 0)
			logger.info("grabber error");

		for (int x = 0; x < H; x++) {
			for (int y = 0; y < W; y++) {
				int pixel = mapPixels[(x * W) + y];
				Color color = new Color(pixel);
				if (Color.white.equals(color)) {
					double pixel_offset = (Math.PI / 150D) / 2D;
					double phi = (((double) x / 150D) * Math.PI) + pixel_offset;
					double theta = (((double) y / 150D) * Math.PI) + Math.PI + pixel_offset;
					if (theta > (2D * Math.PI))
						theta -= (2D * Math.PI);
					result.add(new Coordinates(phi, theta));
				}
			}
		}

		return result;
	}

	/**
	 * Gets all of the mineral concentrations at a given location.
	 * 
	 * @param location the coordinate location.
	 * @return map of mineral types and percentage concentration (0 to 100.0)
	 */
	public Map<String, Double> getAllMineralConcentrations(Coordinates location) {

		if (allMineralsByLocation.isEmpty()
				|| !allMineralsByLocation.containsKey(location)) {
			
			Map<String, Double> map = new HashMap<>();
			
			Iterator<MineralConcentration> i = mineralConcentrations.iterator();
			while (i.hasNext()) {
				MineralConcentration mineralConcentration = i.next();
				double effect = getMineralConcentrationEffect(mineralConcentration, location);
				if (effect > 0D) {
					double totalConcentration = 0D;
					if (map.containsKey(mineralConcentration.getMineralType()))
						totalConcentration = map.get(mineralConcentration.getMineralType());
					totalConcentration += effect;
					if (totalConcentration > 100D)
						totalConcentration = 100D;
					map.put(mineralConcentration.getMineralType(), totalConcentration);
				}
			}
			
			allMineralsByLocation.put(location, map);
			return map;	
		}
		
		return allMineralsByLocation.get(location);
	}

	/**
	 * Gets the mineral concentration at a given location.
	 * 
	 * @param mineralType the mineral type (see MineralMap.java)
	 * @param location    the coordinate location.
	 * @return percentage concentration (0 to 100.0)
	 */
	public double getMineralConcentration(String mineralType, Coordinates location) {
		double result = 0D;

		Iterator<MineralConcentration> i = mineralConcentrations.iterator();
		while (i.hasNext()) {
			MineralConcentration mineralConcentration = i.next();
			if (mineralConcentration.getMineralType().equalsIgnoreCase(mineralType)) {
				result += getMineralConcentrationEffect(mineralConcentration, location);
				if (result > 100D)
					result = 100D;
			}
		}

		return result;
	}

	/**
	 * Gets the effect of a given mineral concentration on a location.
	 * 
	 * @param concentration the mineral concentration.
	 * @param location      the location to affect.
	 * @return concentration effect (0% - 100%).
	 */
	private double getMineralConcentrationEffect(MineralConcentration concentration, Coordinates location) {
		double result = 0D;

		double concentrationPhi = concentration.getLocation().getPhi();
		double concentrationTheta = concentration.getLocation().getTheta();
		double phiDiff = Math.abs(location.getPhi() - concentrationPhi);
		double thetaDiff = Math.abs(location.getTheta() - concentrationTheta);
		double diffLimit = .04D;
		if ((concentrationPhi < Math.PI / 7D) || concentrationPhi > Math.PI - (Math.PI / 7D))
			diffLimit += Math.abs(Math.cos(concentrationPhi));
		if ((phiDiff < diffLimit) && (thetaDiff < diffLimit)) {
			double distance = location.getDistance(concentration.getLocation());
			double concentrationRange = concentration.getConcentration();
			if (distance < concentrationRange)
				result = (1D - (distance / concentrationRange)) * concentration.getConcentration();
		}

		return result;
	}

	/**
	 * Gets an array of all mineral type names.
	 * 
	 * @return array of name strings.
	 */
	public String[] getMineralTypeNames() {
		String[] result = new String[0];
		
		if (mineralMapConfig == null)
			mineralMapConfig = SimulationConfig.instance().getMineralMapConfiguration();

		try {
			List<MineralType> mineralTypes = mineralMapConfig.getMineralTypes();
			result = new String[mineralTypes.size()];
			for (int x = 0; x < mineralTypes.size(); x++)
				result[x] = mineralTypes.get(x).name;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error getting mineral types.", e);
		}

		return result;
	}

	/**
	 * Finds a random location with mineral concentrations from a starting location
	 * and within a distance range.
	 * 
	 * @param startingLocation the starting location.
	 * @param range            the distance range (km).
	 * @return location with one or more mineral concentrations or null if none
	 *         found.
	 */
	public Coordinates findRandomMineralLocation(Coordinates startingLocation, double range) {
		Coordinates result = null;

		List<MineralConcentration> locales = new ArrayList<>(0);

		Iterator<MineralConcentration> i = mineralConcentrations.iterator();
		while (i.hasNext()) {
			MineralConcentration mineralConc = i.next();
			double distance = Coordinates.computeDistance(startingLocation, mineralConc.getLocation());
			if (range > (distance - mineralConc.getConcentration())) {
				locales.add(mineralConc);
			}
		}

		if (locales.size() > 0) {
			int index = RandomUtil.getRandomInt(locales.size() - 1);
			MineralConcentration concentration = locales.get(index);
			double distance = Coordinates.computeDistance(startingLocation, concentration.getLocation());
			if (range < distance) {
				Direction direction = startingLocation.getDirectionToPoint(concentration.getLocation());
				result = startingLocation.getNewLocation(direction, range);
			} else
				result = concentration.getLocation();
		}

		return result;
	}

	@Override
	public void destroy() {
		mineralConcentrations = null;
		allMineralsByLocation = null;
		mineralMapConfig = null;
	}
}
