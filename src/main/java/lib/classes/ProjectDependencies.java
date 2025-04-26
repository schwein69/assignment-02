package lib.classes;

import java.util.List;

public class ProjectDependencies {
  private final List<PackageDependencies> packageReports;

  public ProjectDependencies(List<PackageDependencies> packageReports) {
    this.packageReports = packageReports;
  }

  public List<PackageDependencies> getPackageReports() {
    return packageReports;
  }
}
