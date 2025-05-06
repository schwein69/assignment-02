package lib.classes;

import java.util.Set;

public class ProjectDependencies {
  private final Set<PackageDependencies> packageReports;

  public ProjectDependencies(Set<PackageDependencies> packageReports) {
    this.packageReports = packageReports;
  }

  public Set<PackageDependencies> getPackageReports() {
    return packageReports;
  }

}
