package localization.core.path.filter;

import java.util.ArrayList;
import java.util.Collection;
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

public class Cluster {

	public static List<Pair<String, Set<Integer>>> KMeans(
			List<Pair<String, Set<Integer>>> allPassedMethodWithExecutedMethods, List<Method> collectDataMethods,
			int maxClusterSize, int keepTopN) {
		ArrayList<Attribute> atts = new ArrayList<>();
		for (int i = 0; i < collectDataMethods.size(); i++) {
			Attribute attribute = new Attribute("m" + i);
			atts.add(attribute);
		}
		Instances dataset = new Instances("path", atts, 0);
		for (Pair<String, Set<Integer>> passedTest : allPassedMethodWithExecutedMethods) {
			double[] values = new double[dataset.numAttributes()];
			for (int i = 0; i < collectDataMethods.size(); i++) {
				if (passedTest.getSecond().contains(collectDataMethods.get(i).getMethodID())) {
					values[i] = 1;
				} else {
					values[i] = 0;
				}
			}
			dataset.add(new DenseInstance(1.0, values));
		}

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
			System.out.printf("Instance %d -> Cluster %d \n", i, assignments[i]);
			if(clusters.containsKey(assignments[i])){
				clusters.get(assignments[i]).add(i);
			} else {
				List<Integer> pairIndices = new ArrayList<>();
				pairIndices.add(i);
				clusters.put(assignments[i], pairIndices);
			}
		}
		
		// for debug : output indices for each cluster
		for(Entry<Integer, List<Integer>> entry : clusters.entrySet()){
			System.out.println(entry.getKey() + " --> " + entry.getValue());
		}
		
		List<Pair<String, Set<Integer>>> result = new ArrayList<>();
		for(Entry<Integer, List<Integer>> entry : clusters.entrySet()){
			List<Integer> pairIndices = entry.getValue();
			List<Pair<String, Set<Integer>>> testMethodsInOneCluster = new ArrayList<>();
			for(Integer index : pairIndices){
				testMethodsInOneCluster.add(allPassedMethodWithExecutedMethods.get(index));
			}
			Collections.sort(testMethodsInOneCluster, new Comparator<Pair<String, Set<Integer>>>() {
				@Override
				public int compare(Pair<String, Set<Integer>> o1, Pair<String, Set<Integer>> o2) {
					return o2.getSecond().size() - o1.getSecond().size();
				}
			});
			int maxIndex = keepTopN > testMethodsInOneCluster.size() ? testMethodsInOneCluster.size() : keepTopN;
			for(int i = 0; i < maxIndex; i++){
				result.add(testMethodsInOneCluster.get(i));
			}
		}
		
		return result;
	}

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
				methods.add(random.nextInt(10));
			}
			Pair<String, Set<Integer>> pair = new Pair<String, Set<Integer>>("pass" + i, methods);
			pairList.add(pair);
		}

		List<Method> collectMethods = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			collectMethods.add(new Method(i));
		}
		
		Cluster.KMeans(pairList, collectMethods, 5, 3);

	}

}
