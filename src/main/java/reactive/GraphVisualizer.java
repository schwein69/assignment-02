package reactive;

import lib.classes.ClassDependencies;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.List;


public class GraphVisualizer {

  public Graph<String, DefaultEdge> generateDependencyGraph(List<ClassDependencies> classDependenciesList) {
    Graph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

    // Add nodes and edges to the graph
    for (ClassDependencies classDeps : classDependenciesList) {
      graph.addVertex(classDeps.getClassName());
      for (String dependency : classDeps.getImportedDependencies()) {
        graph.addVertex(dependency);
        graph.addEdge(classDeps.getClassName(), dependency);
      }
    }

    return graph;
  }
}
