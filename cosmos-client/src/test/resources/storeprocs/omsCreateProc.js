function createProc(cosmosDoc){
	var context = getContext();
	var collection = context.getCollection();
	var twoDays = 60 * 60 * 24 * 2;

	function createDoc(context,collection, cosmosDoc){
		var accepted = collection.createDocument(collection.getSelfLink(),
			cosmosDoc,
			function(err,doc){
				if(err) throw err;
				context.getResponse().setBody(doc.id);
			});
		if(!accepted) throw Error("Did not accept create request");
	}


	if(cosmosDoc.cfs['export']){
		var doc = cosmosDoc.cfs['export'];
		var exportDoc = {
			"id" : cosmosDoc["id"]+":"+(new Date().getTime()),
			"oid": cosmosDoc["oid"],
			"timeToLive": twoDays
		};
		
		exportDoc['cfs'] = {
			"export": doc
		};

		createDoc(context,collection,exportDoc);
		cosmosDoc.cfs['export'] = undefined;
	}

	createDoc(context,collection,cosmosDoc);

}

