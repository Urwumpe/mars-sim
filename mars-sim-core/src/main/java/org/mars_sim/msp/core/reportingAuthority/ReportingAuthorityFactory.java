/*
 * Mars Simulation Project
 * ReportingAuthorityFactory.java
 * @date 2023-05-31
 * @author Barry Evans
 */
package org.mars_sim.msp.core.reportingAuthority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.configuration.ConfigHelper;
import org.mars_sim.msp.core.configuration.UserConfigurableConfig;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * Factory method for creating/managing Reporting Authorities.
 * This is loaded via the GovernanceConfig for new simulations
 * or derived from the Settlements in a loaded simulation. 
 */
public final class ReportingAuthorityFactory extends UserConfigurableConfig<ReportingAuthority> {
	
	private final String AUTHORITY_EL = "authority";
	private final String AUTHORITIES_EL = "authorities";
	private final String CODE_ATTR = "code";
	private final String MODIFIER_ATTR = "modifier";
	private final String DESCRIPTION_ATTR = "description";
	private final String CAPABILITY_EL = "capability";
	private final String DATA_ATTR = "data";
	private final String FINDINGS_ATTR = "findings";
	private final String OBJECTIVE_ATTR = "objective";
	private final String AGENDA_EL = "agenda";
	private final String AGENDAS_EL = "agendas";
	private final String COUNTRY_EL = "country";
	private final String NAME_ATTR = "name";
	private final String SETTLEMENTNAME_EL = "settlement-name";
	private final String ROVERNAME_EL = "rover-name";
	private final String GENDER_ATTR = "gender-ratio";
	private final String PERFERENCE_EL = "preference";
	private final String TYPE_ATTR = "type";
	
	private Map<String,MissionAgenda> agendas;

	public ReportingAuthorityFactory(Document governanceDoc) {
		super("authority");
		
		// Load the defaults
		loadGovernanceDetails(governanceDoc);
		
		// Load user defined authorities
		loadUserDefined();
	}

	/**
	 * Loads the Reporting authorities from an external XML.
	 */
	private synchronized void loadGovernanceDetails(Document doc) {
		if (agendas != null) {
			// just in case if another thread is being created
			return;
		}
			
		// Build the global list in a temp to avoid access before it is built
		Map<String,MissionAgenda> newAgendas = new HashMap<>();
		
		// Load the Agendas into a temp Map
		Element agendasNode = doc.getRootElement().getChild(AGENDAS_EL);
		List<Element> agendaNodes = agendasNode.getChildren(AGENDA_EL);
		for (Element agendaNode : agendaNodes) {
			String name = agendaNode.getAttributeValue(NAME_ATTR);
			String objective = agendaNode.getAttributeValue(OBJECTIVE_ATTR);
			String findings = agendaNode.getAttributeValue(FINDINGS_ATTR);
			String data = agendaNode.getAttributeValue(DATA_ATTR);

			// Load sub-agendas
			List<MissionCapability> subs = new ArrayList<>();
			List<Element> subNodes = agendaNode.getChildren(CAPABILITY_EL);
			for (Element subNode : subNodes) {
				String description = subNode.getAttributeValue(DESCRIPTION_ATTR);
	
				// Load the preferences
				Map<PreferenceKey, Double> preferences = new HashMap<>();	
				for (Element preNode : subNode.getChildren(PERFERENCE_EL)) {
					double value = Double.parseDouble(preNode.getAttributeValue(MODIFIER_ATTR));

					// Backward compatiable with the old naming scheme
					String pTypeValue = preNode.getAttributeValue(TYPE_ATTR);
					if (pTypeValue.equals("MISSION") || pTypeValue.equals("TASK")) {
						pTypeValue = pTypeValue + "_WEIGHT";
					}

					PreferenceCategory pType = PreferenceCategory.valueOf(pTypeValue);
					String pName = preNode.getAttributeValue(NAME_ATTR).toUpperCase();

					preferences.put(new PreferenceKey(pType, pName), value);
				}

				subs.add(new MissionCapability(description, 
												Collections.unmodifiableMap(preferences)));
			}	
				
			// Add the agenda
			newAgendas.put(name, new MissionAgenda(name, objective, subs, findings, data));
		}
	
		// Load the Reporting authorities
		Element authoritiesNode = doc.getRootElement().getChild(AUTHORITIES_EL);
		List<Element> authorityNodes = authoritiesNode.getChildren(AUTHORITY_EL);
		for (Element authorityNode : authorityNodes) {
			addItem(parseXMLAuthority(newAgendas, authorityNode, true));
		}
		
		// Assign the agendas
		agendas = Collections.unmodifiableMap(newAgendas);
	}
	
	/**
	 * Parses an Authority XML Element and create a Reporting Authority object.
	 * 
	 * @param authorityNode
	 * @param predefined Is this a repdefined RA
	 * @return
	 */
	private ReportingAuthority parseXMLAuthority(Map<String, MissionAgenda> agendas, Element authorityNode, boolean predefined) {
		String code = authorityNode.getAttributeValue(CODE_ATTR);
		String name = authorityNode.getAttributeValue(NAME_ATTR);
		String agendaName = authorityNode.getAttributeValue(AGENDA_EL);			
		MissionAgenda agenda = agendas.get(agendaName);
		if (agenda == null) {
			 throw new IllegalArgumentException("Agenda called '" + agendaName + "' does not exist for RA " + code);
		}
		double maleRatio = ConfigHelper.getOptionalAttributeDouble(authorityNode, GENDER_ATTR, 0.5D);

		
		// Get Countries
		List<String> countries = authorityNode.getChildren(COUNTRY_EL).stream()
								.map(a -> a.getAttributeValue(NAME_ATTR))
								.collect(Collectors.toList());
		 
		// Get Settlement names
		List<String> settlementNames = authorityNode.getChildren(SETTLEMENTNAME_EL).stream()
				.map(a -> a.getAttributeValue(NAME_ATTR))
				.collect(Collectors.toList());

		// Get Rover names
		List<String> roverNames = authorityNode.getChildren(ROVERNAME_EL).stream()
				.map(a -> a.getAttributeValue(NAME_ATTR))
				.collect(Collectors.toList());
		
		return new ReportingAuthority(code, name, predefined, maleRatio, agenda,
									  countries, settlementNames,
									  roverNames);
	}
	
	/**
	 * Scans the known Settlement and get the load Reporting Authorities. This
	 * makes sure new units will get the same shared Reporting Authority.
	 * What about pending arrivals of new Settlement with new RA ?
	 * 
	 * @param mgr
	 */
	public void discoverReportingAuthorities(UnitManager mgr) {
		// Then overwrite the loaded with those that are active in the simulation
		for (Settlement s : mgr.getSettlements()) {
			ReportingAuthority ra = s.getReportingAuthority();
			addItem(ra);
		}
	}

	/**
	 * Converts a Reporting Authority to an XML representation.
	 */
	@Override
	protected Document createItemDoc(ReportingAuthority item) {
		Element authorityNode = new Element(AUTHORITY_EL);
		authorityNode.setAttribute(CODE_ATTR, item.getName());
		authorityNode.setAttribute(NAME_ATTR, item.getDescription());
		authorityNode.setAttribute(AGENDA_EL, item.getMissionAgenda().getName());	
		authorityNode.setAttribute(GENDER_ATTR, Double.toString(item.getGenderRatio()));
		
		 
		// Get Countries
		addList(authorityNode, COUNTRY_EL, item.getCountries());
		addList(authorityNode, SETTLEMENTNAME_EL, item.getSettlementNames());
		addList(authorityNode, ROVERNAME_EL, item.getVehicleNames());
		
		return new Document(authorityNode);
	}

	private void addList(Element authorityNode, String elName, List<String> items) {
		for (String s : items) {
			Element el = new Element(elName);
			el.setAttribute(NAME_ATTR, s);
			authorityNode.addContent(el);
		}
	}

	/**
	 * Parses a user created XML.
	 */
	@Override
	protected ReportingAuthority parseItemXML(Document doc, boolean predefined) {
		// User configured XML just contains the Authority node.
		return parseXMLAuthority(agendas, doc.getRootElement(), false);
	}

	/**
	 * Gets the names of the known Mission Agendas.
	 * 
	 * @return
	 */
	public List<String> getAgendaNames() {
		List<String> result = new ArrayList<>(agendas.keySet());
		Collections.sort(result);
		return result;
	}

	/**
	 * Finds a defined Mission Agenda by name.
	 * 
	 * @param name
	 * @return
	 */
	public MissionAgenda getAgenda(String name) {
		return agendas.get(name);
	}
}
