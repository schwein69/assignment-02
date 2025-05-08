package lib.classes;

import java.util.HashSet;
import java.util.Set;

public class ClassDependencies {
  private final String className;
  private final Set<String> importedDependencies;
  private final String packageName;

  public ClassDependencies(String className, String packageName, Set<String> dependencies) {
    this.className = className;
    this.packageName = packageName;
    this.importedDependencies = new HashSet<>(dependencies);
  }

  public String getPackageName() {
    return this.packageName;
  }

  public String getClassName() {
    return this.className;
  }

  public Set<String> getImportedDependencies() {
    return this.importedDependencies;
  }

  public void addDependency(String dependency) {
    this.importedDependencies.add(dependency);
  }
}
