import java.util.*;

import jdk.internal.org.objectweb.asm.tree.InnerClassNode;
import parsers.ParseResultsForWS;
import parsers.WebServiceDescription;
import download.WebService;


public class Main {

	public void getRelevantResult(ArrayList<Integer> positions, ArrayList<String[]>  result){


	}
	public ArrayList<String> getArrayOfOutput(String output){
		ArrayList<String> list = new ArrayList<>();

		String[] parts = output.split("\\(");
		String[] part =  parts[1].split(",");

		for (String str: part){
			if (str.endsWith(")"))
				str = str.substring(0, str.length() - 1);
			list.add(str);
		}
		return  list;

	}
	public ArrayList<Integer> findMatchingHeaders(String output, ArrayList<String[]>  qElements){
		ArrayList<Integer> positions = new ArrayList<>();
		ArrayList<String> outputs = new ArrayList<>();
		outputs = getArrayOfOutput(output);

		int i = 0;
		for (String[] listOfString: qElements){
			for (String str: listOfString){
				if (outputs.contains(str))
					positions.add(i);
				i ++;
			}
		}
		return positions;
	}
	public String getNthElement (String tuple[], int n){
		int i = 0;
		for (String str:tuple) {
			if (str == null) continue;
			if (n == i)
				return str;
			if (tuple[i] == null)
				continue;
			i ++;
		}
		return null;
	}
	public Map<String, Set<Integer>> getJoinedElements(ArrayList<String[]>  qElements){
		Map<String, Set<Integer>> map = new HashMap<String, Set<Integer>>();

		ArrayList elements = new ArrayList();
		int i = 0;
		for (String[] qElement:qElements){
			for (String qEl:qElement){
				if (elements.contains(qEl)){
					if (map.get(qEl) == null){
						Set <Integer> set = new HashSet<Integer>() ;
						set.add(elements.indexOf(qEl));
						set.add(i);
						map.put(qEl, set);
					}
					else {
						map.get(qEl).add(i);
					}
				}
				elements.add(qEl);

				i++;
			}
		}


		return map;
	}
	public void deleteNonMacthing(Map<String, Set<Integer>> list, ArrayList<String[]>  newTuples){

		int out = 0;
		Iterator iterator = newTuples.iterator();
		while (iterator.hasNext()){
			String tuple[] = (String[]) iterator.next();
			for (String key:list.keySet()){
				String firstString = getNthElement(tuple,(Integer) list.get(key).toArray()[0]);
				if (out == 1)
					break;
				out = 0;
				for (Integer value:list.get(key)){
					if (!firstString.equals(getNthElement(tuple, value))){
						iterator.remove();
						out = 1;
						break;
					}
				}

			}

		}
	}
	public String setFquery(String fQuery,  WebService ws, Map preList, ArrayList<Integer> currList, ArrayList<String[]>  previousTuples, String[] queryElements, ArrayList<String> curentHeaders){
		int j = 0;
		for (String element:queryElements){
			String dElements = getDelement(ws, j);

			curentHeaders.add(element);
			if (!element.startsWith("?") || currList.contains(j)){
				fQuery += dElements;
				fQuery += ":";
				if (currList.contains(j))
					fQuery += previousTuples.get(0)[(int) preList.get(j) + 1];
				else
					fQuery += element;
				fQuery += " AND ";
			}
			j ++;
		}
		return fQuery;
	}
	public String removeLastAnd (String fQuery){
		return fQuery.substring(0, fQuery.lastIndexOf(" AND"));
	}
	public String getDelement (WebService ws, int j){
		return ws.headVariables.get(j + ws.headVariables.size() - ws.numberOfOutputs).substring(1, ws.headVariables.get(j + ws.headVariables.size() - ws.numberOfOutputs).length());

	}
	public ArrayList<String[]> cloneTuples(ArrayList<String[]> list) {
		/*
		* System.out.println("The tuple results are ");
			for(String [] tuple:listOfTupleResult1){
				System.out.print("( ");
				for(String t:tuple){
					System.out.print(t+", ");
				}
				System.out.print(") ");
				System.out.println();

			}*/
		ArrayList<String[]> clone = new ArrayList<String[]>(list.size());
		for (String[] item : list) clone.add(item.clone());
		return clone;
	}
	public ArrayList<String> cloneHeader(ArrayList<String> list) {

		ArrayList<String> clone = new ArrayList<String>(list.size());
		for (String item : list) clone.add(item);
		return clone;
	}
	public ArrayList<String> addHeaders (ArrayList<String> addTo, ArrayList<String> addFrom) {
		for (String str: addFrom){
			if (!addTo.contains(str))
				addTo.add(str);
		}
		return addTo;
	}
	public String[] extendArray(String[] newTuple, String[] currTuple){
		int lastElement = newTuple.length + 1;
		String[] extended = new String[newTuple.length + currTuple.length];
		int i = 0;
		for(String str:currTuple){
				if (str == null) continue;
				extended[lastElement + i] = str;
				i++;
			}
		System.arraycopy(newTuple, 0, extended, 0, newTuple.length);

		return extended;
	}
	public void cartesianProduct (ArrayList<String[]>  newTuples, ArrayList<String[]>  previousTuples, ArrayList<String[]>  listOfTupleResult){
		for (int z = 0; z < previousTuples.size() ; z++){
			//String[] newTuple = previousTuples.get(z);
			for (String[] currTuple:listOfTupleResult){
				newTuples.add(extendArray(previousTuples.get(z), currTuple));
			}
		}
	}
	public int setCurrentandPreList (ArrayList<Integer> currList, Map preList, String[] queryElements, int joined, String[] preQueryElements) {
		int i = 0;

		for(String el:preQueryElements){
			int k = 0;
			for(String preEl:queryElements){
				if(el.equals(preEl)){
					currList.add(k);
					preList.put(k, i);
					joined = 1;
				}
				k++;
			}
			i ++;
		}
		return joined;
	}
	public static final void main(String[] args) throws Exception{

		Main main = new Main();

		args = new String[] { "P(?id, ?reid)<-mb_getArtistInfoByName(Frank Sinatra,?id,?b,?e)#mb_getAlbumByArtistId(?id,?reid,?release)#mb_getSongByAlbumId(?tid,Frank Sinatra,?recording,2015-12)"};
		//args = new String[] { "P(?title, ?year)<-mb_getSongByAlbumId(0ef6e647-4aeb-438e-8c8a-50c22c511203,?artist,?recording,?date)"};
		//args = new String[] { "P(?title, ?year)<-mb_getArtistInfoByName(Frank Sinatra,?id,?b,?e)"};
		//args = new String[] { "P(?title, ?year)<-mb_getAlbumByArtistId(43bcca8b-9edc-4997-8343-122350e790bf,?reid,?release)"};

		String query = args[0];
		String[] parts = query.split("<-");
		String output = parts[0];
		String[] inputs = parts[1].split("#");
		ArrayList<String[]>  previousTuples = new ArrayList<String[]>();
		ArrayList<String[]>  qElements = new ArrayList<String[]>();
		ArrayList<String[]>  result = new ArrayList<String[]>();
		ArrayList<String> previousHeaders = new ArrayList<String>();
		ArrayList<String> curentHeaders = new ArrayList<String>();
		ArrayList<String> newHeaders = new ArrayList<String>();

		String[] preQueryElements = {};
		int first = 0;
		for (String input:inputs){
			Map preList = new HashMap();
			int joined = 0;
			ArrayList<Integer> currList = new ArrayList<>();
			String[] webService = input.split("\\(");
			WebService ws = WebServiceDescription.loadDescription( webService[0]);

			if (webService[1].endsWith("\\)"))
				System.err.printf("Not well formed query");

			String givenQuery = webService[1].substring(0, webService[1].length() - 1);
			String[] queryElements = givenQuery.split(",");
			qElements.add(queryElements);

			main.setCurrentandPreList(currList, preList, queryElements, joined, preQueryElements);

			preQueryElements = queryElements;

			String fQuery = "";
			fQuery = main.setFquery(fQuery, ws, preList, currList, previousTuples, queryElements, curentHeaders);
			if (fQuery.endsWith(" ")){
				fQuery = main.removeLastAnd(fQuery);
			}

			//if qFquery mpty maybe do smy

			String fileWithCallResult = ws.getCallResult(fQuery);
			String fileWithTransfResults = ws.getTransformationResult(fileWithCallResult);
			ArrayList<String[]>  listOfTupleResult = ParseResultsForWS.showResults(fileWithTransfResults, ws);


			ArrayList<String[]>  newTuples = new ArrayList<String[]>();

			main.addHeaders(newHeaders, curentHeaders);

			main.cartesianProduct(newTuples, previousTuples, listOfTupleResult);

			if (first != 0){
				previousTuples = main.cloneTuples(newTuples);
				result = main.cloneTuples(newTuples);
			}
			else{
				previousTuples = main.cloneTuples(listOfTupleResult);
				result = main.cloneTuples(listOfTupleResult);
			}

			first++;
			previousHeaders = main.cloneHeader(curentHeaders);
			curentHeaders.clear();

		}

		Map<String, Set<Integer>> map = new HashMap<String, Set<Integer>>();

		map = main.getJoinedElements(qElements);

		main.deleteNonMacthing(map, result);

		ArrayList<Integer> positions = new ArrayList<>();

		positions = main.findMatchingHeaders(output, qElements);

		

	}

}
