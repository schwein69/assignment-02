package reactive;

import reactive.controller.DependencyController;
import reactive.model.DependencyModel;
import reactive.view.DependencyView;

public class ReactiveApp {
  public static void main(String[] args) {
    DependencyModel model = new DependencyModel();
    DependencyView view = new DependencyView();
    ReactiveAnalyzer analyzer = new ReactiveAnalyzer();
    new DependencyController(model, view, analyzer);
  }
}
