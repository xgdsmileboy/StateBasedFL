package localization.core.path.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import localization.common.java.Method;
import localization.common.util.LevelLogger;
import localization.common.util.Pair;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 * This class contains some cluster algorithms
 * @author Jiajun
 * @date Mar 24, 2017
 */
public class Cluster {
	
	/**
	 * This method is used for filter passed test cases that used for collecting negative states
	 * @param allPassedMethodWithExecutedMethods
	 * @param collectDataMethods : all methods that will be collecting states
	 * @param maxClusterSize : max size for each cluster
	 * @param keepTopN : keep top N test methods in each cluster
	 * @return
	 */
	public static List<Pair<String, Set<Integer>>> K_Means(
			List<Pair<String, Set<Integer>>> allPassedMethodWithExecutedMethods, List<Method> collectDataMethods,
			int maxClusterSize, int keepTopN) {
		ArrayList<Attribute> atts = new ArrayList<>();
		for (int i = 0; i < collectDataMethods.size(); i++) {
			Attribute attribute = new Attribute("m" + i);
			atts.add(attribute);
		}
		Instances dataset = new Instances("path", atts, 0);
		int[] collectingMethodsCoveredForEachTest = new int[allPassedMethodWithExecutedMethods.size()];
		for(int item = 0; item < allPassedMethodWithExecutedMethods.size(); item++){
			Pair<String, Set<Integer>> passedTest = allPassedMethodWithExecutedMethods.get(item);
			double[] values = new double[dataset.numAttributes()];
			for (int i = 0; i < collectDataMethods.size(); i++) {
				if (passedTest.getSecond().contains(collectDataMethods.get(i).getMethodID())) {
					values[i] = 1;
					collectingMethodsCoveredForEachTest[item] ++;
				} else {
					values[i] = 0;
				}
			}
			dataset.add(new DenseInstance(1.0, values));
		}
//		// output for debugging
//		for(int i = 0; i < allPassedMethodWithExecutedMethods.size(); i++){
//			System.out.print(i + "\t");
//		}
//		System.out.print("\n");
//		for(int i = 0; i < allPassedMethodWithExecutedMethods.size(); i++){
//			System.out.print(collectingMethodsCoveredForEachTest[i] + "\t");
//		}
//		System.out.print("\n");
//		for(int i = 0; i < allPassedMethodWithExecutedMethods.size(); i++){
//			System.out.print(allPassedMethodWithExecutedMethods.get(i).getSecond().size() + "\t");
//		}
//		System.out.print("\n");
		
		int[] assignments = null;
		// set cluster number according to the cluster size
		int clusterNumber = allPassedMethodWithExecutedMethods.size() / maxClusterSize;
		try {
			assignments = computeKMeans(dataset, clusterNumber);
		} catch (Exception e) {
			LevelLogger.error("", e);
			return allPassedMethodWithExecutedMethods;
		}

		Map<Integer, List<Integer>> clusters = new HashMap<>();
		
		for (int i = 0; i < assignments.length; i++) {
			if(clusters.containsKey(assignments[i])){
				clusters.get(assignments[i]).add(i);
			} else {
				List<Integer> pairIndices = new ArrayList<>();
				pairIndices.add(i);
				clusters.put(assignments[i], pairIndices);
			}
		}
		
//		// for debug : output indices for each cluster
//		for(Entry<Integer, List<Integer>> entry : clusters.entrySet()){
//			System.out.println(entry.getKey() + " --> " + entry.getValue());
//		}
		
		List<Pair<String, Set<Integer>>> result = new ArrayList<>();
		for(Entry<Integer, List<Integer>> entry : clusters.entrySet()){
			List<Integer> pairIndices = entry.getValue();
			// record the test method index and the number of methods covered by it (here the methods refer to those used to collect states)
			List<Pair<Integer, Integer>> testMethodIndix2CoveredMethods = new ArrayList<>();
			for(Integer index : pairIndices){
				Pair<Integer, Integer> pair = new Pair<Integer, Integer>(index, collectingMethodsCoveredForEachTest[index]);
				testMethodIndix2CoveredMethods.add(pair);
			}
			Collections.sort(testMethodIndix2CoveredMethods, new Comparator<Pair<Integer, Integer>>() {
				@Override
				public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
					return o2.getSecond() - o1.getSecond();
				}
			});
			int maxIndex = keepTopN > testMethodIndix2CoveredMethods.size() ? testMethodIndix2CoveredMethods.size() : keepTopN;
			for(int i = 0; i < maxIndex; i++){
				int indexOfTestMethod = testMethodIndix2CoveredMethods.get(i).getFirst();
				result.add(allPassedMethodWithExecutedMethods.get(indexOfTestMethod));
			}
		}
		
//		for(Pair<String, Set<Integer>> entry : result){
//			System.out.println(entry.getFirst() + " : " + entry.getSecond());
//		}
		
		return result;
	}

	/**
	 * call k-means cluster in weka
	 * @param dataset
	 * @param clusterNumber
	 * @return
	 * @throws Exception
	 */
	private static int[] computeKMeans(Instances dataset, int clusterNumber) throws Exception {
		SimpleKMeans kmeans = new SimpleKMeans();
		kmeans.setSeed(10);

		// important parameter to set: preserver order, number of cluster.
		kmeans.setPreserveInstancesOrder(true);
		kmeans.setNumClusters(clusterNumber);

		kmeans.buildClusterer(dataset);

		// This array returns the cluster number (starting with 0) for each
		// instance
		// The array has as many elements as the number of instances
		int[] assignments = kmeans.getAssignments();

		return assignments;
	}

	public static void main(String[] args) {
		List<Pair<String, Set<Integer>>> pairList = new ArrayList<>();
		
		Random random = new Random(10);
		for (int i = 0; i < 20; i++) {
			Set<Integer> methods = new HashSet<>();
			for (int j = 0; j < 10; j++) {
				methods.add(random.nextInt(20));
			}
			Pair<String, Set<Integer>> pair = new Pair<String, Set<Integer>>("pass" + i, methods);
			pairList.add(pair);
		}

		List<Method> collectMethods = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			collectMethods.add(new Method(i));
		}
		
		Cluster.K_Means(pairList, collectMethods, 5, 3);

	}

}
