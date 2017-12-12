package com.flipkart.cosmos.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.flipkart.yak.cosmosdb.changefeed.ChangeFeedConsumer;
import com.flipkart.yak.cosmosdb.changefeed.ChangeFeedProcessor;
import com.flipkart.yak.cosmosdb.changefeed.ChangeFeedProcessorImpl;
import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.documentdb.DocumentClientException;

public class ChangeFeedProcessorTest {

	static  String api;
	static  String key;

	static final String dbName = "transact";
	static final String dbPath = "/dbs/"+dbName;
	static final String collName = "orders";
	static final String collPath = dbPath+"/colls/"+collName;

	static ChangeFeedProcessor processor;

	@BeforeClass
	public static void setup() throws DocumentClientException, IOException{
		Properties props = new Properties();
		try(FileInputStream is = new FileInputStream(new File("src/test/resources/changefeed.properties"))){
			props.load(is);
		}
		api = props.getProperty("api");
		key = props.getProperty("key");
		processor = new ChangeFeedProcessorImpl(api, key, collPath,
				new ChangeFeedConsumer() {

			@Override
			public boolean consumer(List<Document> docs) {
				System.out.println("In consumer");
				System.out.println(docs);
				return true;
			}
		});
		System.out.println("Started changeFeedProcessor");

		System.out.println("Finsished setup");
	}



	@AfterClass
	public static void tearDown() throws Exception{
		Thread.sleep(600000);
		processor.shutdown();
		System.out.println("Finished tearDown");
	}
	
	@Test
	public void mockTest(){
		System.out.println("In Mock Test");
	}

}
