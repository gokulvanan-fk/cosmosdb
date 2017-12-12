package com.flipkart.yak.cosmosdb.changefeed;

import java.util.List;

import com.microsoft.azure.documentdb.Document;

public interface ChangeFeedConsumer {

	boolean consume(List<Document> docs);
	
}
