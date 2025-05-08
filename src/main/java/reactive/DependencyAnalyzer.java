package reactive;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lib.classes.ClassDependencies;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class DependencyAnalyzer {
  private static final JLabel classCountLabel = new JLabel("Classes: ");
  private static final JLabel depCountLabel = new JLabel("Dependencies: ");
  private static final Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
  private static final mxGraph mxGraph = new mxGraph();
  private static final mxGraphComponent graphComponent = new mxGraphComponent(mxGraph);
  private static final Set<String> expandedNodes = new HashSet<>();
  private static final Map<String, List<String>> packageToClasses = new HashMap<>();
  private static final Map<String, Set<String>> classToDependencies = new HashMap<>();
  private static final Set<String> expandedPackages = new HashSet<>();


  public static void main(String[] args) {

    JFrame frame = new JFrame("Dependency Analyzer");
    frame.setSize(1200, 800);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    DefaultListModel<String> listModel = new DefaultListModel<>();
    JList<String> jlist = new JList<>(listModel);
    JScrollPane scrollPane = new JScrollPane(jlist);
    scrollPane.setPreferredSize(new Dimension(600, 800));

    frame.getContentPane().add(scrollPane, BorderLayout.WEST);

    ReactiveAnalyzer depReactive = new ReactiveAnalyzer();

    Path[] selectedRoot = {Paths.get(System.getProperty("user.dir"), "src/main/java/lib/classes/")};

    JPanel topPanel = new JPanel(new BorderLayout());
    JLabel selectedPathLabel = new JLabel("Selected: " + selectedRoot[0]);
    JButton srcSelectionButton = getSelectFolderButton(selectedRoot, frame, selectedPathLabel);
    JButton analyzeButton = getAnalyzeButton(listModel, () -> selectedRoot[0], depReactive);
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
    graphComponent.getGraphControl().setPreferredSize(new Dimension(2000, 2000)); // large canvas


    JScrollPane graphScrollPane = new JScrollPane(graphComponent);
    graphScrollPane.setPreferredSize(new Dimension(600, 800));

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, graphScrollPane);
    splitPane.setDividerLocation(600); // 50% screen
    splitPane.setResizeWeight(0.5); // Equal resize on both sides
    frame.getContentPane().add(splitPane, BorderLayout.CENTER);

    frame.setVisible(true);
  }

  private static JButton getSelectFolderButton(Path[] selectedRoot, JFrame frame, JLabel selectedPathLabel) {
    JButton srcSelectionButton = new JButton("Select Folder");
    srcSelectionButton.addActionListener(e -> {
      JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
        selectedRoot[0] = chooser.getSelectedFile().toPath();
        selectedPathLabel.setText("Selected: " + selectedRoot[0]);
      }
    });
    return srcSelectionButton;
  }

  private static JButton getAnalyzeButton(DefaultListModel<String> listModel, Supplier<Path> rootSupplier, ReactiveAnalyzer depReactive) {
    JButton analyzeButton = new JButton("Analyze");

    analyzeButton.addActionListener(e -> {
      listModel.clear();
      graph.removeAllVertices(new java.util.HashSet<>(graph.vertexSet())); // clear previous graph
      graph.removeAllEdges(new java.util.HashSet<>(graph.edgeSet()));

      AtomicInteger classCounter = new AtomicInteger(0);
      AtomicInteger depCounter = new AtomicInteger(0);

      Path root = rootSupplier.get();
      List<Path> javaFiles;
      try (Stream<Path> files = Files.walk(root)) {
        javaFiles = files
          .filter(p -> p.toString().endsWith(".java"))
          .toList();
      } catch (IOException exc) {
        exc.printStackTrace();
        return;
      }
      Observable<ClassDependencies> dependencyObservable = Observable.fromIterable(javaFiles)
        .flatMap(path ->
          depReactive.analyzeClassDependencies(path)
            .subscribeOn(Schedulers.io())
        )
        .observeOn(Schedulers.trampoline());// run observable results in current thread (needed for GUI)


      dependencyObservable.subscribe(
        result -> SwingUtilities.invokeLater(() -> {
          // Add to graph
          String className = result.getClassName();
          Set<String> dependencies = result.getImportedDependencies();
          String packageName = result.getPackageName();
          String projectRoot = "Project";

          graph.addVertex(className);
          classToDependencies.put(className, dependencies); // store raw dependencies
          packageToClasses.computeIfAbsent(packageName, k -> new ArrayList<>()).add(className); // store classes into package
          expandedPackages.add(packageName);


          for (String dep : dependencies) {
            depCounter.incrementAndGet();
          }

          // Package and project nodes
          graph.addVertex(packageName);
          graph.addEdge(packageName, className);  // package → class

          graph.addVertex(projectRoot);
          graph.addEdge(projectRoot, packageName); // project → package

          classCounter.incrementAndGet();
          classCountLabel.setText("Classes: " + classCounter);
          depCountLabel.setText("Dependencies: " + depCounter);
          listModel.addElement(className + " -> " + dependencies);
        }),
        Throwable::printStackTrace,
        () -> SwingUtilities.invokeLater(() -> {
          updateGraphVisualization();
          System.out.println("Analysis complete.");
        })
      );
    });
    return analyzeButton;
  }

  /*private static void updateGraphVisualization() {
    Object parent = DependencyAnalyzer.mxGraph.getDefaultParent();
    DependencyAnalyzer.mxGraph.getModel().beginUpdate();
    try {
      DependencyAnalyzer.mxGraph.removeCells(DependencyAnalyzer.mxGraph.getChildVertices(parent)); // Clear previous content

      // Map to hold the mxGraph cells for each class
      Map<String, Object> vertexMap = new HashMap<>();

      int x = 20, y = 20; // Starting position for node placement
      int stepX = 180, stepY = 60; // Distance between nodes

      // Add vertices from JGraphT to mxGraph
      for (String vertex : DependencyAnalyzer.graph.vertexSet()) {
        Object v = DependencyAnalyzer.mxGraph.insertVertex(parent, null, vertex, x, y, 100, 30);
        vertexMap.put(vertex, v);
        y += stepY;
        if (y > 10000) { // Wrap to next column if too far down
          y = 20;
          x += stepX;
        }
      }

      // Add edges from JGraphT to mxGraph
      for (DefaultEdge edge : DependencyAnalyzer.graph.edgeSet()) {
        String source = DependencyAnalyzer.graph.getEdgeSource(edge);
        String target = DependencyAnalyzer.graph.getEdgeTarget(edge);
        DependencyAnalyzer.mxGraph.insertEdge(parent, null, "", vertexMap.get(source), vertexMap.get(target));
      }
      mxHierarchicalLayout layout = new mxHierarchicalLayout(DependencyAnalyzer.mxGraph);
      layout.execute(parent);
    } finally {
      DependencyAnalyzer.mxGraph.getModel().endUpdate();
    }
  }*/
  private static void updateGraphVisualization() {
    Object parent = mxGraph.getDefaultParent();
    mxGraph.getModel().beginUpdate();
    try {
      mxGraph.removeCells(mxGraph.getChildVertices(parent)); // Clear previous content
      Map<String, Object> vertexMap = new HashMap<>();

      // Add only project, package, and class nodes (not dependencies)
      for (String vertex : graph.vertexSet()) {
        if (vertex.equals("Project") || isPackage(vertex) || isClass(vertex)) {
          Object cell = mxGraph.insertVertex(parent, null, vertex, 0, 0, 100, 30);
          vertexMap.put(vertex, cell);
        }
      }

      for (DefaultEdge edge : graph.edgeSet()) {
        String src = graph.getEdgeSource(edge);
        String tgt = graph.getEdgeTarget(edge);

        // Add only edges to project → package and package → class
        if ((isPackage(src) && isClass(tgt)) || (src.equals("Project") && isPackage(tgt))) {
          if (vertexMap.containsKey(src) && vertexMap.containsKey(tgt)) {
            mxGraph.insertEdge(parent, null, "", vertexMap.get(src), vertexMap.get(tgt));
          }
        }
      }

      mxHierarchicalLayout layout = new mxHierarchicalLayout(mxGraph);
      layout.execute(parent);
    } finally {
      mxGraph.getModel().endUpdate();
    }

    setupExpandOnClick(); // attach listener after graph is ready
  }

  private static void setupExpandOnClick() {
    graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(java.awt.event.MouseEvent e) {
        Object cell = graphComponent.getCellAt(e.getX(), e.getY());
        if (cell != null) {
          String label = (String) mxGraph.getModel().getValue(cell);
          expandNode(label);
        }
      }
    });
  }

  private static void expandNode(String nodeName) {
    if (expandedNodes.contains(nodeName)) return;

    Object parent = mxGraph.getDefaultParent();
    mxGraph.getModel().beginUpdate();
    try {
      Object sourceCell = findCellByLabel(nodeName);

      // If node is a class: show dependencies
      if (classToDependencies.containsKey(nodeName)) {
        for (String dep : classToDependencies.get(nodeName)) {
          graph.addVertex(dep);
          graph.addEdge(nodeName, dep);

          Object depCell = mxGraph.insertVertex(parent, null, dep, 0, 0, 100, 30);
          mxGraph.insertEdge(parent, null, "", sourceCell, depCell);
        }
      }

      // Check if it's a package and not already expanded

      // If node is a package: show classes
      if (packageToClasses.containsKey(nodeName) && !expandedPackages.contains(nodeName)) {
        for (String cls : packageToClasses.get(nodeName)) {
          if (!graph.containsVertex(cls)) {
            graph.addVertex(cls);
          }
          graph.addEdge(nodeName, cls);

          Object clsCell = mxGraph.insertVertex(parent, null, cls, 0, 0, 100, 30);
          mxGraph.insertEdge(parent, null, "", sourceCell, clsCell);
        }
      }


      mxHierarchicalLayout layout = new mxHierarchicalLayout(mxGraph);
      layout.execute(parent);
      expandedNodes.add(nodeName);
    } finally {
      mxGraph.getModel().endUpdate();
    }
  }


  private static boolean isPackage(String s) {
    return s.contains(".") && Character.isLowerCase(s.charAt(0));
  }

  private static boolean isClass(String s) {
    return Character.isUpperCase(s.charAt(0)) && !s.equals("Project");
  }

  private static Object findCellByLabel(String label) {
    Object parent = mxGraph.getDefaultParent();
    for (Object cell : mxGraph.getChildVertices(parent)) {
      if (label.equals(mxGraph.getModel().getValue(cell))) {
        return cell;
      }
    }
    return null;
  }


}
