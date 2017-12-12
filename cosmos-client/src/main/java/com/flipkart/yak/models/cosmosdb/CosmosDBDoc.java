package com.flipkart.yak.models.cosmosdb;

import java.util.Map;

public class CosmosDBDoc {
	public String version; // TODO (gokul) needs better abstraction
	public String oid; //partitionId 
	public String id; // primaryKey;
	public Map<String,CosmosCfs> cfs;
	public Integer timeToLive;
}
