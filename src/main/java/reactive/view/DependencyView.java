package reactive.view;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import javax.swing.*;
import java.awt.*;

public class DependencyView {
  private static final int WIDTH = 1600;
  private static final int HEIGHT = 1000;
  private final JFrame frame = new JFrame("Dependency Analyzer");
  private final DefaultListModel<String> listModel = new DefaultListModel<>();
  private final JLabel classCountLabel = new JLabel("Classes: ");
  private final JLabel depCountLabel = new JLabel("Dependencies: ");
  private final mxGraph mxGraph = new mxGraph();
  private final mxGraphComponent graphComponent = new mxGraphComponent(mxGraph);
  public final JList<String> resultList = new JList<>(listModel);
  public final JLabel selectedPathLabel = new JLabel("Selected: ");
  public final JButton analyzeButton = new JButton("Analyze");
  public final JButton selectFolderButton = new JButton("Select Folder");

  public DependencyView() {
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JScrollPane listScrollPane = new JScrollPane(resultList);
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

  public void clearListModel() {
    this.listModel.clear();
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

  public DefaultListModel<String> getListModel() {
    return listModel;
  }

  public com.mxgraph.view.mxGraph getMxGraph() {
    return mxGraph;
  }

}
