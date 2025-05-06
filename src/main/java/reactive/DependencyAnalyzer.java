package reactive;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DependencyAnalyzer {
  private static final JLabel classCountLabel = new JLabel("Classes: ");
  private static final JLabel depCountLabel = new JLabel("Dependencies: ");

  public static void main(String[] args) {
    JFrame frame = new JFrame("Dependency Analyzer");
    frame.setSize(800, 600);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    DefaultListModel<String> listModel = new DefaultListModel<>();
    JList<String> list = new JList<>(listModel);
    JScrollPane scrollPane = new JScrollPane(list);
    frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

    ReactiveAnalyzer depReactive = new ReactiveAnalyzer();

    Path[] selectedRoot = {Paths.get(System.getProperty("user.dir"), "src/")};

    JPanel topPanel = new JPanel(new BorderLayout());
    JLabel selectedPathLabel = new JLabel("Selected: " + selectedRoot[0]);
    JButton srcSelectionButton = getSelectFolderButton(selectedRoot, frame, selectedPathLabel);
    JButton analyzeButton = getAnalyzeButton(listModel, selectedRoot[0], depReactive);
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

  private static JButton getAnalyzeButton(DefaultListModel<String> listModel, Path root, ReactiveAnalyzer depReactive) {
    JButton analyzeButton = new JButton("Analyze");

    analyzeButton.addActionListener(e -> {
      listModel.clear();
      List<Path> javaFiles;
      try (Stream<Path> files = Files.walk(root)) {
        javaFiles = files
          .filter(p -> p.toString().endsWith(".java"))
          .toList();
      } catch (IOException exc) {
        exc.printStackTrace();
        return;
      }
      Observable.fromIterable(javaFiles)
        .flatMap(path ->
          depReactive.analyzeClassDependencies(path)
            .subscribeOn(Schedulers.io())
        )
        .observeOn(Schedulers.trampoline()) // run observable results in current thread (needed for GUI)
        .subscribe(
          riga -> SwingUtilities.invokeLater(() -> listModel.addElement(riga)),
          Throwable::printStackTrace,
          () -> System.out.println("Analisi completata.")
        );
    });
    return analyzeButton;
  }
}
