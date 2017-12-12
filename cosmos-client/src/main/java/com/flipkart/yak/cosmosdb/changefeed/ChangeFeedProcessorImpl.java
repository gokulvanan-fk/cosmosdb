package com.flipkart.yak.cosmosdb.changefeed;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.documentdb.ChangeFeedOptions;
import com.microsoft.azure.documentdb.ConnectionMode;
import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.FeedOptions;
import com.microsoft.azure.documentdb.FeedResponse;
import com.microsoft.azure.documentdb.PartitionKeyRange;

public class ChangeFeedProcessorImpl implements ChangeFeedProcessor{

	private final DocumentClient client;
	private final ChangeFeedConsumer consumer;
	private final ExecutorService exec;
	private final OffsetStore offsetStore;
	private final Set<ChangeFeedProcessPerPartition> workers;
	private final Logger logger = LoggerFactory.getLogger(ChangeFeedProcessorImpl.class);
	
	public ChangeFeedProcessorImpl(String serviceEndPoint,
			String key,
			String collectionLink,
			ChangeFeedConsumer consumer) {
		ConnectionPolicy policy = new ConnectionPolicy();
		policy.setConnectionMode(ConnectionMode.DirectHttps);

		this.client = new DocumentClient(serviceEndPoint, key, 
				policy, 
				ConsistencyLevel.Strong);
		
		this.exec = Executors.newCachedThreadPool(new ThreadFactory() {
			int i=0;
			@Override
			public Thread newThread(Runnable r) {
				Thread th = new Thread(r, "cosmos-changefeed-"+(i++));
				th.setDaemon(true);
				return th;
			}
		});

		this.offsetStore = new OffsetStoreImpl();
		
		this.workers = new HashSet<>();
		
		this.consumer = consumer;
		FeedOptions opts = new FeedOptions();
		//		opts.setRequestContinuation();
		FeedResponse<PartitionKeyRange> feedResponse = this.client.readPartitionKeyRanges(collectionLink, opts);
		Iterator<PartitionKeyRange> it = feedResponse.getQueryIterator();
		
		while(it.hasNext()){
			PartitionKeyRange partitionKeyRange = it.next();
			logger.info("Got partitionKeyRange {}",partitionKeyRange);
			ChangeFeedProcessPerPartition feedProcessPerPartition = 
					new ChangeFeedProcessPerPartition(partitionKeyRange,
							offsetStore,
							client,
							collectionLink,
							consumer);
			this.exec.execute(feedProcessPerPartition);
			this.workers.add(feedProcessPerPartition);
		}
		
		logger.info("Started ChangeFeedProcessor number of workers {}",workers.size());
	}


	static class ChangeFeedProcessPerPartition implements Runnable {
		private final PartitionKeyRange partitionKeyRange;
		private final OffsetStore offsetStore;
		private final String id;
		private final DocumentClient client;
		private final String collectionLink;
		private final ChangeFeedConsumer consumer;
		private final AtomicBoolean isRunning;
		private static final Logger logger = LoggerFactory.getLogger(ChangeFeedProcessPerPartition.class);
		
		public ChangeFeedProcessPerPartition(PartitionKeyRange partitionKeyRange,
				OffsetStore offsetStore,
				DocumentClient client,
				String collectionLink,
				ChangeFeedConsumer consumer) {
			this.partitionKeyRange = partitionKeyRange;
			this.offsetStore = offsetStore;
			this.collectionLink = collectionLink;
			this.client = client;
			this.consumer = consumer;
			
			this.id = this.collectionLink+"/"+partitionKeyRange.getId();
			this.isRunning = new AtomicBoolean(true);
		}
		
		@Override
		public void run() {
			try{
				while(isRunning.get()){ //Keep running this this is set to false
					logger.info("Running ChangeFeed ");
					ChangeFeedOptions changeFeedOptions = new ChangeFeedOptions();
					changeFeedOptions.setPartitionKeyRangeId(partitionKeyRange.getId());
					changeFeedOptions.setStartFromBeginning(true);
					changeFeedOptions.setRequestContinuation(offsetStore.get(id)); //TODO pull previous read offset from external zookeeper
					changeFeedOptions.setPageSize(20); //TODO move this to config
					FeedResponse<Document> changeFeedResponse = client.queryDocumentChangeFeed(collectionLink, changeFeedOptions);
					Iterator<Document> changeFeedPublisher = changeFeedResponse.getQueryIterator();
					List<Document> docs = new ArrayList<Document>(20);
					while(changeFeedPublisher.hasNext()){
						Document doc = changeFeedPublisher.next();
						logger.info("Got Doc {}",doc);
						docs.add(doc);
					}
					try{
						consumer.consumer(docs);
						offsetStore.store(id, changeFeedResponse.getResponseContinuation());
					}catch(Exception e){
						logger.error("Failed Change Feed processing ",e);
						throw new RuntimeException(e); // TODO better exception handling needed
					}
				}
			}finally{
				logger.info("Finished running ChangeFeed");
				isRunning.compareAndSet(true, false);
			}
		}

		//TODO have a better way to shutdown
		public void stop(){
			isRunning.compareAndSet(true, false);
		}

		
	}

	@Override
	public void shutdown() {
		for(ChangeFeedProcessPerPartition runner : workers){
			runner.stop();
		}
		this.exec.shutdown();
	}



}
