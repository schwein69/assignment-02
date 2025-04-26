package lib.classes;

import java.util.List;

public class PackageDependencies {
  private final String packageName;
  private final List<ClassDependencies> classReports;

  public PackageDependencies(String packageName, List<ClassDependencies> classReports) {
    this.packageName = packageName;
    this.classReports = classReports;
  }

  public String getPackageName() {
    return packageName;
  }

  public List<ClassDependencies> getClassReports() {
    return classReports;
  }
}
