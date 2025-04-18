package lib.classes;

import java.util.List;

public class ClassDependencies {
  private final String className;
  private final String packageName;
  private final List<String> dependencies;

  public ClassDependencies(String className, String packageName, List<String> dependencies) {
    this.className = className;
    this.packageName = packageName;
    this.dependencies = dependencies;
  }

  public String getClassName() {
    return className;
  }

  public String getPackageName() {
    return packageName;
  }

  public List<String> getDependencies() {
    return dependencies;
  }
}
