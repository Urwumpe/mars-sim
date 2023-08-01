package org.mars_sim.msp.core.structure.goods;

import java.util.List;

import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.equipment.BinType;
import org.mars_sim.msp.core.equipment.EquipmentType;
import org.mars_sim.msp.core.goods.Good;
import org.mars_sim.msp.core.goods.GoodType;
import org.mars_sim.msp.core.goods.GoodsManager;
import org.mars_sim.msp.core.goods.GoodsUtil;
import org.mars_sim.msp.core.resource.ItemResource;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.ResourceUtil;

import junit.framework.TestCase;

public class TestGoods extends TestCase {

    ItemResource hammer;
    
    public TestGoods() {
		super();
	}

    protected void setUp() throws Exception {
        SimulationConfig config = SimulationConfig.instance();
        config.loadConfig();
        ResourceUtil.getInstance();   
        
        // Don't need a full GoodsManager initialisation
        GoodsManager.initializeInstances(config, null, null);

        GoodType type = GoodType.TOOL;
        hammer = ItemResourceUtil.createItemResource("hammer", 1100, "a hand tool", type, 1.4D, 1);
    }

    public void testCreateItem() {
    	GoodType type = GoodType.INSTRUMENT;
    	Part microlens = ItemResourceUtil.createItemResource("microlens", 1102, "a test lense", type, 0.05D, 1);
    	assertNotNull(microlens);
    }

    public void testGoodsListNotNull() {
        List<Good> goodsList = GoodsUtil.getGoodsList();
        assertNotNull(goodsList);
	}

	public void testGoodsListNotZero() {
        List<Good> goodsList = GoodsUtil.getGoodsList();
		assertTrue(goodsList.size() > 0);
	}

	public void testGoodsListContainsWater() {
		Good waterGood = GoodsUtil.getGood(ResourceUtil.waterID);
		assertNotNull("Found water Good", waterGood);
	}

	public void testGoodsListContainsHammer() {
        Good hammerGood = GoodsUtil.getGood(hammer.getID());
        // hammer is not a standardized part and is NOT registered on the goodsMap
        assertNull(hammerGood);
	}

	public void testGoodsListContainsBag() {
		Good bagGood = GoodsUtil.getEquipmentGood(EquipmentType.BAG);
		assertNotNull("Found Bag Good", bagGood);
	}

	public void testGoodsListContainsPot() {
		Good potGood = GoodsUtil.getBinGood(BinType.POT);
		assertNotNull("Found Pot Good", potGood);
	}
	
	public void testGoodsListContainsExplorerRover() {
		// "Explorer Rover" is a valid vehicle type
        String typeName = "Explorer Rover";
		Good explorerRoverGood = GoodsUtil.getVehicleGood(typeName);
		assertNotNull("Found good vehicleType " +  typeName, explorerRoverGood);
	}

	public void testGoodsListDoesntContainFalseRover() {
		// "False Rover" is not a valid vehicle type
		Good falseRoverGood = GoodsUtil.getVehicleGood("False Rover");
		assertNull("Non-Existent Vehicle Good not found", falseRoverGood);
	}
}