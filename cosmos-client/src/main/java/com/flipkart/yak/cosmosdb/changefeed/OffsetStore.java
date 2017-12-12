package com.flipkart.yak.cosmosdb.changefeed;

public interface OffsetStore {

	void store(String key, String offset);
	
	String get(String key);
}
