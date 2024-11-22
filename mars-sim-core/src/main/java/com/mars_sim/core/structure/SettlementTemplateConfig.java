package com.mars_sim.core.structure;

import com.mars_sim.core.configuration.ConfigHelper;
import com.mars_sim.core.configuration.UserConfigurableConfig;
import com.mars_sim.core.interplanetary.transport.resupply.ResupplyConfig;
import com.mars_sim.core.interplanetary.transport.resupply.ResupplySchedule;
import com.mars_sim.core.map.location.BoundedObject;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.person.ai.shift.ShiftPattern;
import com.mars_sim.core.resource.*;
import com.mars_sim.core.robot.RobotTemplate;
import com.mars_sim.core.robot.RobotType;
import com.mars_sim.core.structure.building.BuildingPackageConfig;
import com.mars_sim.core.structure.building.BuildingTemplate;
import org.jdom2.Document;
import org.jdom2.Element;

import java.util.*;
import java.util.logging.Logger;

public class SettlementTemplateConfig extends UserConfigurableConfig<SettlementTemplate> {

    private static final Logger logger = Logger.getLogger(SettlementConfig.class.getName());

    private static final String BUILDING_PACKAGE = "building-package";
    private static final String BUILDING = "building";
    private static final String CONNECTOR = "connector";
    private static final String STANDALONE = "standalone";

    private static final String SETTLEMENT_TEMPLATE_LIST = "settlement-template-list";
    private static final String TEMPLATE = "template";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String DEFAULT_POPULATION = "default-population";
    private static final String DEFAULT_NUM_ROBOTS = "number-of-robots";
    private static final String OBJECTIVE = "objective";

    private static final String ID = "id";
    private static final String HATCH_FACE = "hatch-facing";
    private static final String ZONE = "zone";
    private static final String TYPE = "type";
    private static final String CONNECTION_LIST = "connection-list";
    private static final String CONNECTION = "connection";
    private static final String NUMBER = "number";
    private static final String VEHICLE = "vehicle";
    private static final String EQUIPMENT = "equipment";
    private static final String BIN = "bin";
    private static final String SPONSOR = "sponsor";
    private static final String RESUPPLY = "resupply";
    private static final String RESUPPLY_MISSION = "resupply-mission";
    private static final String AMOUNT = "amount";
    private static final String PART = "part";
    private static final String PART_PACKAGE = "part-package";
    private static final String RESOURCE = "resource";

    private static final String SHIFT_PATTERN = "shift-pattern";
    private static final String MODEL = "model";
    private static final String MANIFEST_NAME = "manifest-name";
    private static final String SCHEDULE = "schedule";
    private static final String ACTIVITY_SCHEDULE = "activity-schedule";

    private static final String EVA = "EVA";
    private static final String ROBOT = "robot";

    private final PartPackageConfig partPackageConfig;
    private final BuildingPackageConfig buildingPackageConfig;
    private final ResupplyConfig resupplyConfig;

    private final SettlementConfig settlementConfig;

    /**
     * Constructor.
     *
     * @param settlementDoc     DOM document with settlement configuration.
     * @param partPackageConfig the part package configuration.
     * @param settlementConfig
     */
    public SettlementTemplateConfig(Document settlementDoc,
                                    PartPackageConfig partPackageConfig,
                                    BuildingPackageConfig buildingPackageConfig,
                                    ResupplyConfig resupplyConfig, SettlementConfig settlementConfig) {
        super("settlement");
        this.partPackageConfig = partPackageConfig;
        this.buildingPackageConfig = buildingPackageConfig;
        this.resupplyConfig = resupplyConfig;
        this.settlementConfig = settlementConfig;
        setXSDName("settlement.xsd");

        Element root = settlementDoc.getRootElement();

        String [] defaults = loadSettlementTemplates(settlementDoc);

        loadDefaults(defaults);

        loadUserDefined();
    }


    /**
     * Loads the settlement templates from the XML document.
     *
     * @param settlementDoc     DOM document with settlement configuration.
     * @throws Exception if error reading XML document.
     */
    private String[] loadSettlementTemplates(Document settlementDoc) {
        Element root = settlementDoc.getRootElement();
        Element templateList = root.getChild(SETTLEMENT_TEMPLATE_LIST);
        List<Element> templateNodes = templateList.getChildren(TEMPLATE);
        List<String> names = new ArrayList<>();

        for (Element templateElement : templateNodes) {
            names.add(templateElement.getAttributeValue(NAME));
        }
        return names.toArray(new String[0]);
    }

    /**
     * Parses the building or connector list.
     *
     * @param templateElement
     * @param settlementTemplate
     * @param settlementTemplateName
     * @param existingStreetIDs
     */
    private void parseBuildingORConnectorList(Element templateElement, SettlementTemplate settlementTemplate,
                                              String elementName,
                                              String settlementTemplateName,
                                              Set<String> existingStreetIDs,
                                              Map<String, Integer> buildingTypeNumMap) {

        List<Element> buildingNodes = templateElement.getChildren(elementName);
        for (Element buildingElement : buildingNodes) {

            BoundedObject bounds = ConfigHelper.parseBoundedObject(buildingElement);

            // Track the id
            String id = "";

            if (buildingElement.getAttribute(ID) != null) {
                id = buildingElement.getAttributeValue(ID);
            }

            if (existingStreetIDs.contains(id)) {
                throw new IllegalStateException(
                        "Error in SettlementConfig: the id " + id + " in settlement template "
                                + settlementTemplateName + " is not unique.");
            } else if (!id.equalsIgnoreCase("")) {
                existingStreetIDs.add(id);
            }

            // Assume the zone as 0
            int zone = ConfigHelper.getOptionalAttributeInt(buildingElement, ZONE, 0);

            // Get the building type
            String buildingType = buildingElement.getAttributeValue(TYPE);

            int last = getNextBuildingTypeID(buildingType, buildingTypeNumMap);

            // e.g. Lander Hab 1, Lander Hab 2
            String uniqueName = buildingType + " " + last;

            BuildingTemplate buildingTemplate = new BuildingTemplate(id, zone,
                    buildingType, uniqueName, bounds);

            // Need to check for collision with previous building templates
            for (BuildingTemplate t: settlementTemplate.getBuildings()) {
                BoundedObject o0 = buildingTemplate.getBounds();
                BoundedObject o1 = t.getBounds();
                if (BoundedObject.isCollided(o0, o1)) {
                    throw new IllegalStateException(uniqueName + " collided with " + t.getBuildingName()
                            + " in settlement template: " + settlementTemplateName + ".");
                }
            }

            settlementTemplate.addBuildingTemplate(buildingTemplate);

            // Create building connection templates.
            Element connectionListElement = buildingElement.getChild(CONNECTION_LIST);
            if (connectionListElement != null) {
                List<Element> connectionNodes = connectionListElement.getChildren(CONNECTION);
                for (Element connectionElement : connectionNodes) {
                    String connectionID = connectionElement.getAttributeValue(ID);

                    if (buildingType.equalsIgnoreCase(EVA)) {
                        buildingTemplate.addEVAAttachedBuildingID(connectionID);
                    }

                    // Check that connection ID is not the same as the building ID.
                    if (connectionID.equalsIgnoreCase(id)) {
                        throw new IllegalStateException(
                                "Connection ID cannot be the same as id for this building/connector "
                                        + buildingType
                                        + " in settlement template: " + settlementTemplateName + ".");
                    }

                    String hatchFace = connectionElement.getAttributeValue(HATCH_FACE);

                    if (hatchFace == null) {
                        LocalPosition connectionLoc = ConfigHelper.parseLocalPosition(connectionElement);
                        buildingTemplate.addBuildingConnection(connectionID, connectionLoc);
                    }
                    else {
                        buildingTemplate.addBuildingConnection(connectionID, hatchFace);
                    }
                }
            }
        }
    }

    /**
     * Gets an available building type suffix ID for a new building.
     *
     * @param buildingType
     * @return type ID (starting from 1, not zero)
     */
    private int getNextBuildingTypeID(String buildingType, Map<String, Integer> buildingTypeIDMap) {
        int last = 1;
        if (buildingTypeIDMap.containsKey(buildingType)) {
            last = buildingTypeIDMap.get(buildingType);
            buildingTypeIDMap.put(buildingType, ++last);
        } else {
            buildingTypeIDMap.put(buildingType, last);
        }
        return last;
    }

    @Override
    protected SettlementTemplate parseItemXML(Document doc, boolean predefined) {
        Element templateElement = doc.getRootElement();

        String settlementTemplateName = templateElement.getAttributeValue(NAME);
        String description = templateElement.getAttributeValue(DESCRIPTION);
        String sponsor = templateElement.getAttributeValue(SPONSOR);

        // Obtains the default population
        int defaultPopulation = Integer.parseInt(templateElement.getAttributeValue(DEFAULT_POPULATION));
        // Obtains the default numbers of robots
        int defaultNumOfRobots = Integer.parseInt(templateElement.getAttributeValue(DEFAULT_NUM_ROBOTS));

        // Look up the shift pattern
        ShiftPattern pattern = null;
        String shiftPattern = templateElement.getAttributeValue(SHIFT_PATTERN);
        if (shiftPattern == null) {
            pattern = settlementConfig.getShiftByPopulation(defaultPopulation);
        }
        else {
            pattern = settlementConfig.getShiftByName(shiftPattern);
        }

        GroupActivitySchedule activitySchedule = null;
        String scheduleName = templateElement.getAttributeValue(ACTIVITY_SCHEDULE);
        if (scheduleName != null) {
            activitySchedule = settlementConfig.getActivityByName(scheduleName);
        }
        else {
            activitySchedule = settlementConfig.getActivityByPopulation(defaultPopulation);
        }

        // Add templateID
        SettlementTemplate settlementTemplate = new SettlementTemplate(
                settlementTemplateName,
                description,
                predefined,
                sponsor,
                pattern,
                activitySchedule,
                defaultPopulation,
                defaultNumOfRobots);

        // Check the objective
        String objectiveText = templateElement.getAttributeValue(OBJECTIVE);
        if (objectiveText != null) {
            var oType = ConfigHelper.getEnum(ObjectiveType.class, objectiveText);
            settlementTemplate.setObjective(oType);
        }

        Set<String> existingBuildingIDs = new HashSet<>();
        Map<String, Integer> buildingTypeNumMap = new HashMap<>();

        // Process a list of buildings
        parseBuildingORConnectorList(templateElement, settlementTemplate,
                BUILDING,
                settlementTemplateName,
                existingBuildingIDs,
                buildingTypeNumMap);

        // Process a list of connectors
        Set<String> existingConnectorIDs = new HashSet<>();
        parseBuildingORConnectorList(templateElement, settlementTemplate,
                CONNECTOR,
                settlementTemplateName,
                existingConnectorIDs,
                buildingTypeNumMap);

        // Process a list of standalone buildings
        parseBuildingORConnectorList(templateElement, settlementTemplate,
                STANDALONE,
                settlementTemplateName,
                existingBuildingIDs,
                buildingTypeNumMap);


        // Check that building connections point to valid building ID's.
        List<BuildingTemplate> buildingTemplates = settlementTemplate.getBuildings();

        for (BuildingTemplate buildingTemplate : buildingTemplates) {

            List<BuildingTemplate.BuildingConnectionTemplate> connectionTemplates = buildingTemplate
                    .getBuildingConnectionTemplates();

            for (BuildingTemplate.BuildingConnectionTemplate connectionTemplate : connectionTemplates) {

                if (!existingBuildingIDs.contains(connectionTemplate.getID())
                        && !existingConnectorIDs.contains(connectionTemplate.getID())) {
                    throw new IllegalStateException("XML issues with settlement template: "
                            + settlementTemplateName
                            + " in " + buildingTemplate.getBuildingName()
                            + " at connection id " + connectionTemplate.getID()
                            + ". existingBuildingIDs: " + existingBuildingIDs
                            + ". existingConnectorIDs: " + existingConnectorIDs
                    );
                }
            }
        }

        // Load vehicles
        List<Element> vehicleNodes = templateElement.getChildren(VEHICLE);
        for (Element vehicleElement : vehicleNodes) {
            String vehicleType = vehicleElement.getAttributeValue(TYPE);
            int vehicleNumber = Integer.parseInt(vehicleElement.getAttributeValue(NUMBER));
            settlementTemplate.addVehicles(vehicleType, vehicleNumber);
        }

        // Load robots
        List<Element> robotNodes = templateElement.getChildren(ROBOT);
        for (Element robotElement : robotNodes) {
            RobotType rType = ConfigHelper.getEnum(RobotType.class,
                    robotElement.getAttributeValue(TYPE));
            String name = robotElement.getAttributeValue(NAME);
            String model = robotElement.getAttributeValue(MODEL);
            settlementTemplate.addRobot(new RobotTemplate(name, rType, model));
        }

        // Load equipment
        List<Element> equipmentNodes = templateElement.getChildren(EQUIPMENT);
        for (Element equipmentElement : equipmentNodes) {
            String equipmentType = equipmentElement.getAttributeValue(TYPE);
            int equipmentNumber = Integer.parseInt(equipmentElement.getAttributeValue(NUMBER));
            settlementTemplate.addEquipment(equipmentType, equipmentNumber);
        }

        // Load bins
        List<Element> binNodes = templateElement.getChildren(BIN);
        for (Element binElement : binNodes) {
            String binType = binElement.getAttributeValue(TYPE);
            int binNumber = Integer.parseInt(binElement.getAttributeValue(NUMBER));
            settlementTemplate.addBins(binType, binNumber);
        }

        // Load resources
        List<Element> resourceNodes = templateElement.getChildren(RESOURCE);
        for (Element resourceElement : resourceNodes) {
            String resourceType = resourceElement.getAttributeValue(TYPE);
            AmountResource resource = ResourceUtil.findAmountResource(resourceType);
            if (resource == null)
                logger.severe(resourceType + " shows up in settlement template "
                        + settlementTemplateName
                        + " but doesn't exist in resources.xml.");
            else {
                double resourceAmount = Double.parseDouble(resourceElement.getAttributeValue(AMOUNT));
                settlementTemplate.addAmountResource(resource, resourceAmount);
            }
        }

        // Load parts
        List<Element> partNodes = templateElement.getChildren(PART);
        for (Element partElement : partNodes) {
            String partType = partElement.getAttributeValue(TYPE);
            Part part = (Part) ItemResourceUtil.findItemResource(partType);
            if (part == null)
                logger.severe(partType + " shows up in settlement template "
                        + settlementTemplateName
                        + " but doesn't exist in parts.xml.");
            else {
                int partNumber = Integer.parseInt(partElement.getAttributeValue(NUMBER));
                settlementTemplate.addPart(part, partNumber);
            }
        }

        // Load part packages
        List<Element> partPackageNodes = templateElement.getChildren(PART_PACKAGE);
        for (Element partPackageElement : partPackageNodes) {
            String packageName = partPackageElement.getAttributeValue(NAME);
            int packageNumber = Integer.parseInt(partPackageElement.getAttributeValue(NUMBER));
            if (packageNumber > 0) {
                for (int z = 0; z < packageNumber; z++) {
                    Map<Part, Integer> partPackage = partPackageConfig.getPartsInPackage(packageName);
                    for (Part part : partPackage.keySet()) {
                        int partNumber = partPackage.get(part);
                        settlementTemplate.addPart(part, partNumber);
                    }
                }
            }
        }

        // Load building packages
        List<Element> buildingPackageNodes = templateElement.getChildren(BUILDING_PACKAGE);
        for (Element buildingPackageElement : buildingPackageNodes) {
            String packageName = buildingPackageElement.getAttributeValue(NAME);

            List<BuildingTemplate> buildingPackages = buildingPackageConfig.getBuildingsInPackage(packageName);

            for (BuildingTemplate buildingTemplate: buildingPackages) {

                // Get the building type
                String buildingType = buildingTemplate.getBuildingType();

                int last = getNextBuildingTypeID(buildingType, buildingTypeNumMap);

                String uniqueName = buildingType + " " + last;

                // Overwrite with a new building nick name
                buildingTemplate.setBuildingName(uniqueName);

                settlementTemplate.addBuildingTemplate(buildingTemplate);
            }
        }

        // Load resupplies
        Element resupplyList = templateElement.getChild(RESUPPLY);
        if (resupplyList != null) {
            List<Element> resupplyNodes = resupplyList.getChildren(RESUPPLY_MISSION);
            for (Element resupplyMissionElement : resupplyNodes) {
                String resupplyName = resupplyMissionElement.getAttributeValue(NAME);
                String manifestName = resupplyMissionElement.getAttributeValue(MANIFEST_NAME);
                ResupplyConfig.SupplyManifest manifest = resupplyConfig.getSupplyManifest(manifestName);

                var schedule = ConfigHelper.parseEventCalendar(resupplyMissionElement.getChild(SCHEDULE));
                ResupplySchedule resupplyMissionTemplate = new ResupplySchedule(resupplyName, schedule, manifest);
                settlementTemplate.addResupplyMissionTemplate(resupplyMissionTemplate);
            }
        }

        return settlementTemplate;
    }

    /**
     * It is not possible to create new SettlementTemplates via the application.
     */
    @Override
    protected Document createItemDoc(SettlementTemplate item) {
        throw new UnsupportedOperationException("Saving Settlement templates is not supported.");
    }
}