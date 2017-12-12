


function deleteProc(docId,version){
	var context = getContext();
	var collection = context.getCollection();

	//Internal functions 

	// function bin2String(array) {
	//   var result = "";
	//   for (var i = 0; i < array.length; i++) {
	//     result += String.fromCharCode(parseInt(array[i], 2));
	//   }
	//   return result;
	// }


	function checkAndDelete(context, collection, docId, version){
		readDoc(context, collection, docId, function(existingDoc){
			var existingVersion = existingDoc.cfs['data'].dataMap["version"]
			if(existingVersion != version){
				throw new Error("Cas Mismatch error, expected version "
					+(existingVersion)+" got version "+(version));
			}
			deleteDoc(context, collection,existingDoc);
		});
	}


	function deleteDoc(context,collection, existingDoc){
		var accepted = collection.deleteDocument(existingDoc._self,
			function(err,doc){
				if(err) throw err;
			});
		if(!accepted) throw Error("Did not accept update request");
	}

	function readDoc(context,collection, docId, callback){
		var accepted = collection.readDocument(collection.getAltLink()+"/docs/"+docId,
			function(err,doc){
				if(err) throw err;
				callback(doc);
			});
		if(!accepted) throw Error("Did not accept update request");
	}


	//TODO (gokul) change this to more generic object for future use case
	checkAndDelete(context, collection, docId, version);
	

}



