package reactive;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import reactive.model.Dependencies;

public class DependencyAnalyzer {
  private static final int WIDTH = 1400;
  private static final int HEIGHT = 800;
  private static final JLabel classCountLabel = new JLabel("Classes: ");
  private static final JLabel depCountLabel = new JLabel("getDependencies: ");
  private static final Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
  private static final mxGraph mxGraph = new mxGraph();
  private static final mxGraphComponent graphComponent = new mxGraphComponent(mxGraph);
  private static final AtomicInteger classCounter = new AtomicInteger(0);
  private static final AtomicInteger depCounter = new AtomicInteger(0);
  private static Path srcPath = null;
  private static final String projectRoot = "src";
  private static final Set<String> addedEdges = new HashSet<>();


  public static void main(String[] args) {

    JFrame frame = new JFrame("Dependency Analyzer");
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    DefaultListModel<String> listModel = new DefaultListModel<>();
    JList<String> jlist = new JList<>(listModel);
    JScrollPane scrollPane = new JScrollPane(jlist);
    scrollPane.setPreferredSize(new Dimension((int) (WIDTH * 0.5), HEIGHT));

    frame.getContentPane().add(scrollPane, BorderLayout.WEST);

    ReactiveAnalyzer depReactive = new ReactiveAnalyzer();

    JPanel topPanel = new JPanel(new BorderLayout());
    JLabel selectedPathLabel = new JLabel("Selected: ");
    JButton srcSelectionButton = getSelectFolderButton(frame, selectedPathLabel);
    JButton analyzeButton = getAnalyzeButton(listModel, depReactive);
    topPanel.add(srcSelectionButton, BorderLayout.WEST);
    topPanel.add(selectedPathLabel, BorderLayout.CENTER);
    topPanel.add(analyzeButton, BorderLayout.EAST);
    frame.add(topPanel, BorderLayout.NORTH);

    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
    classCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    depCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    bottomPanel.add(classCountLabel);
    bottomPanel.add(depCountLabel);
    frame.add(bottomPanel, BorderLayout.SOUTH);

    // Create a graph component from the JGraphT graph and add it to the graphPanel
    // Add a panel to visualize the graph
    graphComponent.setConnectable(false);
    graphComponent.setAutoScroll(true);
    graphComponent.getGraphControl().setPreferredSize(new Dimension(2000, 2000)); // large canvas

    JScrollPane graphScrollPane = new JScrollPane(graphComponent);
    graphScrollPane.setPreferredSize(new Dimension((int) (WIDTH * 0.5), HEIGHT));

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, graphScrollPane);
    splitPane.setDividerLocation((int) (WIDTH * 0.5));
    splitPane.setResizeWeight(0.5); // Equal resize on both sides
    frame.getContentPane().add(splitPane, BorderLayout.CENTER);

    frame.setVisible(true);
  }

  private static JButton getSelectFolderButton(JFrame frame, JLabel selectedPathLabel) {
    JButton srcSelectionButton = new JButton("Select Folder");
    srcSelectionButton.addActionListener(e -> {
      JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
        srcPath = chooser.getSelectedFile().toPath();
        selectedPathLabel.setText("Selected: " + srcPath);
      }
    });
    return srcSelectionButton;
  }

  private static void reset(DefaultListModel<String> listModel) {
    listModel.clear();
    graph.removeAllVertices(new HashSet<>(graph.vertexSet())); // clear previous graph
    graph.removeAllEdges(new HashSet<>(graph.edgeSet()));
    vertexMap.clear();
    addedEdges.clear();
    mxGraph.getModel().beginUpdate();
    try {
      mxGraph.removeCells(mxGraph.getChildVertices(mxGraph.getDefaultParent()));
    } finally {
      mxGraph.getModel().endUpdate();
    }
  }

  private static JButton getAnalyzeButton(DefaultListModel<String> listModel, ReactiveAnalyzer depReactive) {
    JButton analyzeButton = new JButton("Analyze");
    analyzeButton.addActionListener(e -> {
      if (srcPath == null) {
        throw new IllegalArgumentException();
      }
      reset(listModel);
      List<Path> javaFiles;
      try (Stream<Path> files = Files.walk(srcPath)) {
        javaFiles = files
          .filter(p -> p.toString().endsWith(".java"))
          .toList();
      } catch (IOException exc) {
        exc.printStackTrace();
        return;
      }
      Flowable<Dependencies> dependencyObservable = Flowable.fromIterable(javaFiles)
        .flatMap(path ->
          depReactive.analyzeDependencies(path)
            .subscribeOn(Schedulers.io())
        )
        .observeOn(Schedulers.trampoline());// For gui

      dependencyObservable.subscribe(
        result -> SwingUtilities.invokeLater(() -> {
          String className = result.className();
          Set<String> dependencies = result.getDependencies();
          String packageName = result.packageName();
          Path srcPathParent = result.srcPath().getParent().getParent();

          // Add class node
          graph.addVertex(className);
          //classToDependencies.put(className, dependencies); // store raw dependencies
          //packageToClasses.computeIfAbsent(packageName, k -> new ArrayList<>()).add(className); // store classes into package

          for (String dep : dependencies) {
            graph.addVertex(dep);
            graph.addEdge(className, dep);
            depCounter.incrementAndGet();
          }

          // Package and project nodes
          graph.addVertex(packageName);
          graph.addEdge(packageName, className);  // package → class

          Path nextSrcPathParent = srcPathParent;
          if (!packageName.equals(projectRoot)) {
            // Add parent package to package.
            while (nextSrcPathParent != null && !nextSrcPathParent.getFileName().toString().equals(projectRoot)) { //java -> lib -> classes
              String folderName = nextSrcPathParent.getFileName().toString();
              graph.addVertex(folderName);
              graph.addEdge(folderName, packageName);
              packageName = folderName;
              nextSrcPathParent = nextSrcPathParent.getParent();
            }
          }

          /*Add java root node*/
          graph.addVertex(projectRoot);
          graph.addEdge(projectRoot, packageName); // project → package

          classCounter.incrementAndGet();
          classCountLabel.setText("Classes: " + classCounter);
          depCountLabel.setText("getDependencies: " + depCounter);
          listModel.addElement(className + " -> " + dependencies);

          updateGraphVisualizationIncremental();

        }),
        Throwable::printStackTrace
      );
    });
    return analyzeButton;
  }

  private static final Map<String, Object> vertexMap = new HashMap<>();

  private static void updateGraphVisualizationIncremental() {
    Object parent = mxGraph.getDefaultParent();
    mxGraph.getModel().beginUpdate();
    try {
      // Add only new vertices
      for (String vertex : graph.vertexSet()) {
        if (!vertexMap.containsKey(vertex)) {
          Object cell = mxGraph.insertVertex(parent, null, vertex, 0, 0, 100, 30);
          vertexMap.put(vertex, cell);
        }
      }

      // Add only new edges
      for (DefaultEdge edge : graph.edgeSet()) {
        String src = graph.getEdgeSource(edge);
        String tgt = graph.getEdgeTarget(edge);

        String edgeKey = src + "->" + tgt;
        if (src != null && tgt != null && !addedEdges.contains(edgeKey)) {
          mxGraph.insertEdge(parent, null, "", vertexMap.get(src), vertexMap.get(tgt));
          addedEdges.add(edgeKey);
        }

      }

      // Layout only once (optional: only if new things added)
      mxHierarchicalLayout layout = new mxHierarchicalLayout(mxGraph);
      layout.setOrientation(SwingConstants.NORTH);
      layout.setIntraCellSpacing(30);
      layout.setInterRankCellSpacing(100);
      layout.execute(parent);

      graphComponent.zoomAndCenter();
    } finally {
      mxGraph.getModel().endUpdate();
    }
  }

}
