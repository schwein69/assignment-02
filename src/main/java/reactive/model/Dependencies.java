package reactive.model;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public record Dependencies(String className, String packageName, Set<String> getDependencies, Path srcPath) {


  @Override
  public String className() {
    return className;
  }

  @Override
  public Set<String> getDependencies() {
    return new HashSet<>(getDependencies);
  }

  @Override
  public String packageName() {
    return packageName;
  }

  @Override
  public Path srcPath() {
    return srcPath;
  }
}
