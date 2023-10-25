/*
 * Mars Simulation Project
 * ScienceConfig.java
 * @date 2021-08-28
 * @author Manny Kung
 */

package com.mars_sim.core.science;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.tools.util.RandomUtil;
 
public class ScienceConfig implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
 
	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(ScienceConfig.class.getName());

	private final String SCIENTIFIC_STUDY = "scientific_study";
	private final String JSON = "json";
	private final String DIR = "/";
	private final String SUBJECT = "subject";
	private final String TOPIC = "topic";
	private final String TOPICS = TOPIC + "s";
	private final String DOT = ".";
	private final String UNDERSCORE = "_";
	private final String GENERAL = "General";
	
	private final String JSON_DIR = DIR + JSON + DIR;

	private final String TOPICS_JSON_FILE_EXT = UNDERSCORE + TOPICS + DOT + JSON;
	private final String SCIENTIFIC_STUDY_JSON = SCIENTIFIC_STUDY + DOT + JSON;
	
	private String[] jsonFiles = new String[ScienceType.valuesList().size()]; 
    
    private static List<Integer> averageTime = new ArrayList<>(); 
    
    private static int aveNumCollaborators;

	private static int maxStudiesPerPerson = 2;
        
    private Map<ScienceType, List<Topic>> scienceTopics = new EnumMap<>(ScienceType.class);

    /**
     * Constructor.
     */
    public ScienceConfig() {
        InputStream fis = null;
        JsonReader jsonReader = null;
        JsonObject jsonObject = null;
        
        // Load the scientific study param json files
        fis = this.getClass().getResourceAsStream(JSON_DIR + SCIENTIFIC_STUDY_JSON);
        jsonReader = Json.createReader(fis);

        // Get JsonObject from JsonReader
        jsonObject = jsonReader.readObject();
         
        // Close IO resource and JsonReade
        jsonReader.close();
        try {
			fis.close();
		} catch (IOException e1) {
          	logger.log(Level.SEVERE, "Cannot close json file: "+ e1.getMessage());
		}
         
        aveNumCollaborators = jsonObject.getInt("average_num_collaborators");
        maxStudiesPerPerson = jsonObject.getInt("max_studies_per_person");

        averageTime.add(jsonObject.getInt("base_proposal_time"));
        averageTime.add(jsonObject.getInt("base_primary_research_study_time"));
        averageTime.add(jsonObject.getInt("base_collaborative_research_study_time"));
        averageTime.add(jsonObject.getInt("base_primary_researcher_paper_writing_time"));
        averageTime.add(jsonObject.getInt("base_collaborator_paper_writing_time"));
        averageTime.add(jsonObject.getInt("peer_review_time"));
        averageTime.add(jsonObject.getInt("primary_researcher_work_downtime_allowed"));
        averageTime.add(jsonObject.getInt("collaborator_work_downtime_allowed"));
    	     
    	// Create a list of science topic filenames 
    	createJsonFiles();
    	
        // Load the topic json files
    	for (String fileName : jsonFiles) {  
	        fis = this.getClass().getResourceAsStream(fileName);
	        jsonReader = Json.createReader(fis);
	         
	        // Get JsonObject from JsonReader
	        try {
	        	jsonObject = jsonReader.readObject();
	        }  
	        catch (RuntimeException rte) {
	        	logger.severe("Problem parsing JSON " + fileName);
	        	throw rte;
	        }
	        
	        // Close IO resource and JsonReader
	        jsonReader.close();
	        try {
				fis.close();
			} catch (IOException e1) {
	          	logger.log(Level.SEVERE, "Cannot close json file: "+ e1.getMessage());
			}
	         
	        Subject s = new Subject();
	        // Retrieve a subject from JsonObject
	        s.setSubject(jsonObject.getString(SUBJECT));
	     
//	        s.setNum(jsonObject.getInt("numbers"));
//	        int size = s.getNum();
	        
	        // Read the json array of topics
	        JsonArray jsonArray = jsonObject.getJsonArray(TOPICS);
	        
	        int size = jsonArray.size();
	        
	        s.setNum(size);
	        
	        try {
		        for (int i = 0; i< size; i++) {
	                JsonObject child = jsonArray.getJsonObject(i);
		        	s.createTopic(child.getString(TOPIC));
		        }
	        } catch (Exception e1) {
	          	logger.log(Level.SEVERE, "Cannot get json object: "+ e1.getMessage());
			}
     
	        scienceTopics.put(ScienceType.getType(s.getName()), s.getTopics());
    	}
    }
 
    
    public void createJsonFiles() {
    	int size = ScienceType.valuesList().size();
    	for (int i=0; i<size; i++) {
    		ScienceType type = ScienceType.valuesList().get(i);
    		jsonFiles[i] = JSON_DIR + type.getName().toLowerCase() + TOPICS_JSON_FILE_EXT;
    	}
    }
    
    
    public String getATopic(ScienceType type) {
    	if (scienceTopics.containsKey(type)) {
    		List<Topic> topics = scienceTopics.get(type);
    		int size = topics.size();
    		if (size > 0) {
	    		int num = RandomUtil.getRandomInt(size-1);
	    		return topics.get(num).getName();
    		}
    	}
    	return GENERAL;	
    }
    
    public static int getAverageTime(int index) {
    	return averageTime.get(index); 
    }
    
    static int getAveNumCollaborators() {
    	return aveNumCollaborators;
    }

	public static int getMaxStudies() {
		return maxStudiesPerPerson ;
	}

    /**
     * The scientific subject holding a list of topics.
     */
	class Subject {
	    
		int num;
		
		String name;

		List<Topic> topics = new ArrayList<>();
		
		Subject() {}

		void setSubject(String value) {
			this.name = value;
		}
		
		void setNum(int num) {
			this.num = num;
		}
		
		int getNum() {
			return num;
		}
		
		String getName() {
			return name;
		}
		
	   	void createTopic(String name) {
	   		Topic e = new Topic(name);
	   		topics.add(e);
    	}
	   	
	   	List<Topic> getTopics() {
	   		return topics;
	   	}
	}
	
    /**
     * The Topic of a Subject.
     */
    class Topic {
    
    	String name;
    	String id;
    	
    	Topic(String name) {
    		this.name = name;
    	}
    	
    	String getName() {
    		return name;
    	}
    	
    }
    
    /**
     * Prepares object for garbage collection.
     */
    public void destroy() {
        jsonFiles = null;
        averageTime.clear();
        averageTime = null; 
        scienceTopics.clear();
        scienceTopics = null;   
    }
}
