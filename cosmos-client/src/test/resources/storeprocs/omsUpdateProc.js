

function updateProc(cosmosDoc){
	var context = getContext();
	var collection = context.getCollection();
	var twoDays = 60 * 60 * 24 * 2;

	//Internal functions 

	// function bin2String(array) {
	//   var result = "";
	//   for (var i = 0; i < array.length; i++) {
	//     result += String.fromCharCode(parseInt(array[i], 2));
	//   }
	//   return result;
	// }


	function checkAndUpdate(context, collection, cosmosDoc){
		readDoc(context, collection, cosmosDoc, function(existingDoc){
			var version = cosmosDoc.version;
			var existingVersion = existingDoc.cfs['data'].dataMap["version"]
			if(existingVersion != version){
				throw new Error("Cas Mismatch error, expected version "
					+existingVersion+" got version "+(version));
			}
			updateDoc(context, collection, cosmosDoc, existingDoc);
		});
	}

	function createDoc(context,collection, cosmosDoc){
		var accepted = collection.createDocument(collection.getSelfLink(),
			cosmosDoc,
			function(err,doc){
				if(err) throw err;
				context.getResponse().setBody(doc.id);
			});
		if(!accepted) throw Error("Did not accept create request");
	}

	function updateDoc(context,collection, cosmosDoc, existingDoc){
		var accepted = collection.replaceDocument(existingDoc._self,
			cosmosDoc,
			function(err,doc){
				if(err) throw err;
			});
		if(!accepted) throw Error("Did not accept update request");
	}

	function readDoc(context,collection, cosmosDoc, callback){
		var accepted = collection.readDocument(collection.getAltLink()+"/docs/"+cosmosDoc.id,
			function(err,doc){
				if(err) throw err;
				callback(doc);
			});
		if(!accepted) throw Error("Did not accept update request");
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

	checkAndUpdate(context,collection,cosmosDoc);
	

}



