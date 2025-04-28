import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lib.DependencyAnalyserLib;
import lib.classes.ClassDependencies;
import lib.classes.PackageDependencies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestClassDependencies {
  @Test
  public void testClassDependencies(Vertx vertx, VertxTestContext vertxTestContext) {
    DependencyAnalyserLib analyser = new DependencyAnalyserLib(vertx);
    Path path = Path.of(System.getProperty("user.dir"), "src/main/java/lib/DependencyAnalyserLib.java");
    String normalizedPath = path.toString().replace("\\", "/");

    analyser.getClassDependencies(Path.of(normalizedPath))
      .onComplete(result -> {
        if (result.succeeded()) {
          ClassDependencies report = result.result();
          System.out.println("Package: " + report.getPackageName());
          System.out.println("Class: " + report.getClassName());
          System.out.println("Dependencies: " + report.getDependencies());
          assertEquals("DependencyAnalyserLib", report.getClassName());
          vertxTestContext.completeNow();
        } else {
          result.cause().printStackTrace();
          vertxTestContext.failNow(result.cause());
        }
      });
  }

  @Test
  public void testPackageDependencies(Vertx vertx, VertxTestContext vertxTestContext) {
    DependencyAnalyserLib analyser = new DependencyAnalyserLib(vertx);
    Path path = Path.of(System.getProperty("user.dir"), "src/main/java/lib/classes");
    String normalizedPath = path.toString().replace("\\", "/");

    analyser.getPackageDependencies(Path.of(normalizedPath))
      .onComplete(result -> {
        if (result.succeeded()) {
          PackageDependencies report = result.result();
          System.out.println("Package: " + report.getPackageName());
          report.getClassReports().forEach(classDep -> {
            System.out.println("Class " + classDep.getClassName() + " depends on: " + classDep.getDependencies());
          });
          vertxTestContext.completeNow();
        } else {
          result.cause().printStackTrace();
          vertxTestContext.failNow(result.cause());
        }
      });
  }
}
