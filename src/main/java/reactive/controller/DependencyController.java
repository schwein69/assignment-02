package reactive.controller;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.jgrapht.graph.DefaultEdge;
import reactive.ReactiveAnalyzer;
import reactive.model.Dependencies;
import reactive.model.DependencyModel;
import reactive.view.DependencyView;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class DependencyController {
  private Path srcPath;

  public DependencyController(DependencyModel model, DependencyView view, ReactiveAnalyzer analyzer) {
    view.selectFolderButton.addActionListener(e -> {
      JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      if (chooser.showOpenDialog(view.getFrame()) == JFileChooser.APPROVE_OPTION) {
        srcPath = chooser.getSelectedFile().toPath();
        view.selectedPathLabel.setText("Selected: " + srcPath);
      }
    });

    view.analyzeButton.addActionListener(e -> {
      if (srcPath == null) return;
      reset(model, view);


      Flowable<Dependencies> dependencyObservable = analyzePath(analyzer, srcPath);

      dependencyObservable.subscribe(result -> SwingUtilities.invokeLater(() -> {
        String className = result.className();
        String packageName = result.packageName();
        Set<String> dependencies = result.getDependencies();
        Path srcParent = result.srcPath().getParent().getParent();

        model.getGraph().addVertex(className);
        for (String dep : dependencies) {
          model.addVertex(dep);
          model.addEdge(className, dep);
          model.incrementDependenciesCounter();
        }

        model.addVertex(packageName);
        model.addEdge(packageName, className);
        List<String> listPackages = new LinkedList<>();
        listPackages.add(packageName);
        if (srcParent != null && !packageName.equals(model.getProjectRoot())) {
          while (srcParent != null && !packageName.equals(model.getProjectRoot())) {
            String folder = srcParent.getFileName().toString();
            model.addVertex(folder);
            model.addEdge(folder, packageName);
            listPackages.addFirst(folder);
            packageName = folder;
            srcParent = srcParent.getParent();
          }
          view.addPackageToPackage(listPackages, className, dependencies);
        } else {
          view.addNodeToJTree(result.packageName(), className, dependencies);

        }


        view.updateTreeModel();
        view.expandAllNodes();

        model.addVertex(model.getProjectRoot());
        model.addEdge(model.getProjectRoot(), packageName);

        model.incrementClassCounter();
        view.getClassCountLabel().setText("Classes: " + model.getClassCounter());
        view.getDepCountLabel().setText("Dependencies: " + model.getDependenciesCounter());

        updateGraphView(model, view);
      }), Throwable::printStackTrace);
    });
  }

  private void reset(DependencyModel model, DependencyView view) {
    view.clearTree();
    model.reset();
    view.getMxGraph().getModel().beginUpdate();
    try {
      view.getMxGraph().removeCells(view.getMxGraph().getChildVertices(view.getMxGraph().getDefaultParent()));
    } finally {
      view.getMxGraph().getModel().endUpdate();
    }
  }

  private Flowable<Dependencies> analyzePath(ReactiveAnalyzer depReactive, Path root) {
    return Flowable.using(
      () -> Files.walk(root),
      files -> Flowable.fromStream(files)
        .filter(p -> p.toString().endsWith(".java"))
        .flatMap(path -> depReactive.analyzeDependencies(path)
          .subscribeOn(Schedulers.io())),
      Stream::close
    ).observeOn(Schedulers.computation());
  }

  private void updateGraphView(DependencyModel model, DependencyView view) {
    var parent = view.getMxGraph().getDefaultParent();
    view.getMxGraph().getModel().beginUpdate();
    try {
      for (String vertex : model.getGraph().vertexSet()) {
        if (!model.getVertexMap().containsKey(vertex)) {
          Object cell = view.getMxGraph().insertVertex(parent, null, vertex, 0, 0, 100, 30);
          model.getVertexMap().put(vertex, cell);
        }
      }

      for (DefaultEdge edge : model.getGraph().edgeSet()) {
        String src = model.getGraph().getEdgeSource(edge);
        String tgt = model.getGraph().getEdgeTarget(edge);
        String key = src + "->" + tgt;
        if (!model.getAddedEdges().contains(key)) {
          view.getMxGraph().insertEdge(parent, null, "", model.getVertexMap().get(src), model.getVertexMap().get(tgt));
          model.getAddedEdges().add(key);
        }
      }

      mxHierarchicalLayout layout = new mxHierarchicalLayout(view.getMxGraph());
      layout.setOrientation(SwingConstants.NORTH);
      layout.setIntraCellSpacing(30);
      layout.setInterRankCellSpacing(100);
      layout.execute(parent);
      view.getGraphComponent().zoomAndCenter();
    } finally {
      view.getMxGraph().getModel().endUpdate();
    }
  }
}
