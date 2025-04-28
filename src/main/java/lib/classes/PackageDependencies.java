package lib.classes;

import java.util.Set;

public class PackageDependencies {
  private final String packageName;
  private final Set<ClassDependencies> classReports;

  public PackageDependencies(String packageName, Set<ClassDependencies> classReports) {
    this.packageName = packageName;
    this.classReports = classReports;
  }

  public String getPackageName() {
    return packageName;
  }

  public Set<ClassDependencies> getClassReports() {
    return classReports;
  }
}
