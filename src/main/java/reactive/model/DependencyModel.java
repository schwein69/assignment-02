package reactive.model;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class DependencyModel {
  private final Graph<String, DefaultEdge> graph;
  private final Map<String, Object> vertexMap;
  private final Set<String> addedEdges;
  private final String projectRoot = "src";
  private int classCounter;
  private int dependenciesCounter;

  public DependencyModel() {
    this.classCounter = 0;
    this.dependenciesCounter = 0;
    this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    this.vertexMap = new HashMap<>();
    this.addedEdges = new HashSet<>();
  }

  public Map<String, Object> getVertexMap() {
    return this.vertexMap;
  }

  public Set<String> getAddedEdges() {
    return this.addedEdges;
  }

  public Graph<String, DefaultEdge> getGraph() {
    return this.graph;
  }

  public String getProjectRoot() {
    return this.projectRoot;
  }

  public void addEdge(String folder, String packageName) {
    this.graph.addEdge(folder, packageName);
  }

  public void addVertex(String folder) {
    this.graph.addVertex(folder);
  }

  public void reset() {
    this.graph.removeAllVertices(new HashSet<>(graph.vertexSet()));
    this.graph.removeAllEdges(new HashSet<>(graph.edgeSet()));
    this.vertexMap.clear();
    this.addedEdges.clear();
    this.classCounter = 0;
    this.dependenciesCounter = 0;
  }

  public int getClassCounter() {
    return this.classCounter;
  }

  public void incrementClassCounter() {
    this.classCounter += 1;
  }

  public void incrementDependenciesCounter() {
    this.dependenciesCounter += 1;
  }

  public int getDependenciesCounter() {
    return this.dependenciesCounter;
  }
}
