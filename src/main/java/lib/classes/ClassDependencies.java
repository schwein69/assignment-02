package lib.classes;

import java.util.Set;

public class ClassDependencies {
  private final String className;
  private final Set<String> importedDependencies;
  private final String packageName;

  public ClassDependencies(String className, String packageName, Set<String> dependencies) {
    this.className = className;
    this.packageName = packageName;
    this.importedDependencies = dependencies;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getClassName() {
    return className;
  }

  public Set<String> getImportedDependencies() {
    return importedDependencies;
  }
}
