import java.util.*;

import parsers.ParseResultsForWS;
import parsers.WebServiceDescription;
import download.WebService;
import static java.lang.System.exit;

public class Main {
    public static ArrayList<String[]> previousTuples;
    public static ArrayList<String[]> newTuples;
    public static ArrayList<String[]> result;
    public static String[] queryElements;
    public static ArrayList<String> curentHeaders;
    public static ArrayList<String> previousHeaders;
    public static WebService ws;
    public static String output;
    public static String[] inputs;

    //printing results
    public void printResults(ArrayList<String[]>  result, int showNTuples){
        ArrayList<String> outputs = getArrayOfOutput();
        if (result.size() < showNTuples)
            showNTuples = result.size();
        for(int i = 0; i < showNTuples; i++){
            int j = 0;
            for (String out:outputs){
                System.out.println(out + " = " + result.get(i)[j]);
                j++;
            }
        }
    }
    // projection of columns
	public ArrayList<String[]> getRelevantResult(ArrayList<String[]>  result, ArrayList<String[]> qElements){
		ArrayList<String> newStr = new ArrayList<>();
		for(String[] strList: qElements){
            Collections.addAll(newStr, strList);
		}
		int position = -1;
		ArrayList<String[]>  newResult = new ArrayList<>();
		ArrayList<String> outputs = getArrayOfOutput();

		for (String[] tuple: result){
			int i = 0;
			String[] newTuple = new String[tuple.length];
			for (String out:outputs){
                position = newStr.indexOf(out);
				if (position == -1){
					System.err.printf("Query does not contains desired outputs.");
				    exit(0);
				}
				else{
					String toAdd = getNthElement(tuple, position);
					newTuple[i] = toAdd;
				}
                i++;
            }
            newResult.add(newTuple);
        }
		return newResult;

	}
	// returning output string as an arraylist
	public ArrayList<String> getArrayOfOutput(){
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
	//get N th elementh of a tuple
	public String getNthElement (String tuple[], int n){
		int i = 0;
		for (String str:tuple) {
			if (str == null) continue;
			if (n == i)
				return str;
			if (tuple[i] == null){
				i++;
				continue;}
			i ++;
		}
		return null;
	}
	// find joined items in the query
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
	// delete non matching tuples
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
	// setting outcall query
	public String setFquery(String fQuery, Map preList, ArrayList<Integer> currList){
		int j = 0;
		for (String element:queryElements){
			String dElements = getDelement(j);

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
	//remove last and
	public String removeLastAnd (String fQuery){
		return fQuery.substring(0, fQuery.lastIndexOf(" AND"));
	}
	//getting element from web service description
	public String getDelement (int j){
		return ws.headVariables.get(j + ws.headVariables.size() - ws.numberOfOutputs).substring(1, ws.headVariables.get(j + ws.headVariables.size() - ws.numberOfOutputs).length());

	}
	//clon tuples
	public ArrayList<String[]> cloneTuples(ArrayList<String[]> list) {
		ArrayList<String[]> clone = new ArrayList<>(list.size());
		for (String[] item : list) clone.add(item.clone());
		return clone;
	}
	//clone headers
	public ArrayList<String> cloneHeader(ArrayList<String> list) {

		ArrayList<String> clone = new ArrayList<String>(list.size());
		for (String item : list) clone.add(item);
		return clone;
	}
	// add headers
	public ArrayList<String> addHeaders (ArrayList<String> addTo, ArrayList<String> addFrom) {
		for (String str: addFrom){
			if (!addTo.contains(str))
				addTo.add(str);
		}
		return addTo;
	}
	// extending an array
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
	// cartesian product of given tuples
	public void cartesianProduct (ArrayList<String[]>  newTuples, ArrayList<String[]>  previousTuples, ArrayList<String[]>  listOfTupleResult){
		for (int z = 0; z < previousTuples.size() ; z++){
			for (String[] currTuple:listOfTupleResult){
				newTuples.add(extendArray(previousTuples.get(z), currTuple));
			}
		}
	}
	public int setCurrentandPreList (ArrayList<Integer> currList, Map preList, int joined, String[] preQueryElements) {
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

		//Testing with multiple cases
		//args = new String[] { "P(?id,?reid)<-mb_getArtistInfoByName(Frank Sinatra,?id,?b,?e)#mb_getAlbumByArtistId(?id,?reid,?release)#mb_getSongByAlbumId(?tid,Frank Sinatra,?recording,2015-12)"};
		//args = new String[] { "P(?artist,?date)<-mb_getSongByAlbumId(0ef6e647-4aeb-438e-8c8a-50c22c511203,?artist,?recording,?date)"};
		//args = new String[] { "P(?id,?b)<-mb_getArtistInfoByName(Frank Sinatra,?id,?b,?e)"};
		//args = new String[] { "P(?id,?b)<-mb_getArtistInfoByName(Mert Ozer,?id,?b,?e)"};
		//args = new String[] { "P(?reid,?release)<-mb_getAlbumByArtistId(43bcca8b-9edc-4997-8343-122350e790bf,?reid,?release)"};

		//splitting input and output
		String query = args[0];
		String[] parts = query.split("<-");
		output = parts[0];
		inputs = parts[1].split("#");
		previousTuples = new ArrayList<>();
		ArrayList<String[]>  qElements = new ArrayList<>();
		ArrayList<String[]>  result = new ArrayList<>();
		previousHeaders = new ArrayList<>();
		curentHeaders = new ArrayList<>();
		ArrayList<String> newHeaders = new ArrayList<>();

		String[] preQueryElements = {};
		int first = 0;
		//for every element we have in the input
		for (String input:inputs){
			Map preList = new HashMap();
			int joined = 0;
			ArrayList<Integer> currList = new ArrayList<>();
			String[] webService = input.split("\\(");
			ws = WebServiceDescription.loadDescription( webService[0]);

			if (webService[1].endsWith("\\)"))
				System.err.printf("Not well formed query");

			String givenQuery = webService[1].substring(0, webService[1].length() - 1);
			queryElements = givenQuery.split(",");
			qElements.add(queryElements);

			main.setCurrentandPreList(currList, preList, joined, preQueryElements);

			preQueryElements = queryElements;

			String fQuery = "";
			fQuery = main.setFquery(fQuery, preList, currList);
			if (fQuery.endsWith(" ")){
				fQuery = main.removeLastAnd(fQuery);
			}

			String fileWithCallResult = ws.getCallResult(fQuery);
			String fileWithTransfResults = ws.getTransformationResult(fileWithCallResult);
			ArrayList<String[]>  listOfTupleResult = ParseResultsForWS.showResults(fileWithTransfResults, ws);

			newTuples = new ArrayList<>();

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

		Map<String, Set<Integer>> map = main.getJoinedElements(qElements);

		main.deleteNonMacthing(map, result);

		ArrayList<String[]> desiredOutput = main.getRelevantResult(result, qElements);

        main.printResults(desiredOutput, 25);

	}

}
