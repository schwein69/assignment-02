package reactive.view;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.*;
import java.util.List;


public class DependencyView {
  private static final int WIDTH = 1600;
  private static final int HEIGHT = 1000;
  private final JFrame frame = new JFrame("Dependency Analyzer");
  private final JLabel classCountLabel = new JLabel("Classes: ");
  private final JLabel depCountLabel = new JLabel("Dependencies: ");
  private final mxGraph mxGraph = new mxGraph();
  private final mxGraphComponent graphComponent = new mxGraphComponent(mxGraph);
  public final JLabel selectedPathLabel = new JLabel("Selected: ");
  public final JButton analyzeButton = new JButton("Analyze");
  public final JButton selectFolderButton = new JButton("Select Folder");
  private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
  private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
  private final JTree jTree = new JTree(treeModel);

  public DependencyView() {
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JScrollPane listScrollPane = new JScrollPane(jTree);
    JScrollPane graphScrollPane = new JScrollPane(graphComponent);

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, graphScrollPane);
    splitPane.setDividerLocation(WIDTH / 2);
    frame.getContentPane().add(splitPane, BorderLayout.CENTER);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(selectFolderButton, BorderLayout.WEST);
    topPanel.add(selectedPathLabel, BorderLayout.CENTER);
    topPanel.add(analyzeButton, BorderLayout.EAST);

    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
    bottomPanel.add(classCountLabel);
    bottomPanel.add(depCountLabel);

    frame.add(topPanel, BorderLayout.NORTH);
    frame.add(bottomPanel, BorderLayout.SOUTH);

    graphComponent.setConnectable(false);
    graphComponent.setAutoScroll(true);

    frame.setVisible(true);
  }


  public void addNodeToJTree(String nodeParentPackage, String nodeChildClass, Set<String> dependencies) {
    DefaultMutableTreeNode parentPackage = findOrCreateChild(root, nodeParentPackage);

    // Check if class already exists under this package
    DefaultMutableTreeNode existingClassNode = findChild(parentPackage, nodeChildClass);
    if (existingClassNode == null) {
      DefaultMutableTreeNode childClass = new DefaultMutableTreeNode(nodeChildClass);

      // Add dependency nodes
      for (String dep : dependencies) {
        childClass.add(new DefaultMutableTreeNode(dep));
      }

      parentPackage.add(childClass);
    }
  }

  public void addPackageToPackage(List<String> packages, String nodeChildClass, Set<String> dependencies) {
    DefaultMutableTreeNode current = root;

    // Traverse or build package path
    for (String pkg : packages) {
      current = findOrCreateChild(current, pkg);
    }

    // Add class node with dependencies
    DefaultMutableTreeNode classNode = findChild(current, nodeChildClass);
    if (classNode == null) {
      classNode = new DefaultMutableTreeNode(nodeChildClass);
      for (String dep : dependencies) {
        classNode.add(new DefaultMutableTreeNode(dep));
      }
      current.add(classNode);
    }
  }


  private DefaultMutableTreeNode findOrCreateChild(DefaultMutableTreeNode parent, String childName) {
    Enumeration<?> children = parent.children();
    while (children.hasMoreElements()) {
      Object node = children.nextElement();
      if (node instanceof DefaultMutableTreeNode child) {
        if (childName.equals(child.getUserObject())) {
          return child;
        }
      }
    }

    // Not found — create new
    DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(childName);
    parent.add(newChild);
    return newChild;
  }

  private DefaultMutableTreeNode findChild(DefaultMutableTreeNode parent, String childName) {
    Enumeration<?> children = parent.children();
    while (children.hasMoreElements()) {
      Object node = children.nextElement();
      if (node instanceof DefaultMutableTreeNode child) {
        if (childName.equals(child.getUserObject())) {
          return child;
        }
      }
    }
    return null;
  }

  public void updateTreeModel() {
    this.treeModel.reload();
  }

  public void clearTree() {
    root.removeAllChildren();
    ((DefaultTreeModel) jTree.getModel()).reload();
  }

  public JLabel getClassCountLabel() {
    return classCountLabel;
  }

  public JLabel getDepCountLabel() {
    return depCountLabel;
  }

  public JFrame getFrame() {
    return frame;
  }

  public mxGraphComponent getGraphComponent() {
    return graphComponent;
  }

  public mxGraph getMxGraph() {
    return mxGraph;
  }

  public void expandAllNodes() {
    int row = 0;
    while (row < jTree.getRowCount()) {
      jTree.expandRow(row);
      row++;
    }
  }


}
