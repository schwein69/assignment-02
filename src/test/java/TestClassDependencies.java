import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lib.DependencyAnalyserLib;
import lib.classes.ClassDependencies;
import lib.classes.PackageDependencies;
import lib.classes.ProjectDependencies;
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
          System.out.println("getDependencies: " + report.getImportedDependencies());
          assertEquals("DependencyAnalyserLib", report.getClassName());
          vertxTestContext.completeNow();
        } else {
          result.cause().printStackTrace();
          vertxTestContext.failNow(result.cause());
        }
      });
  }

  @Test
  public void testClassDependencies2(Vertx vertx, VertxTestContext vertxTestContext) {
    DependencyAnalyserLib analyser = new DependencyAnalyserLib(vertx);
    Path path = Path.of(System.getProperty("user.dir"), "src/main/java/lib/classes/ClassDependencies.java");
    String normalizedPath = path.toString().replace("\\", "/");

    analyser.getClassDependencies(Path.of(normalizedPath))
      .onComplete(result -> {
        if (result.succeeded()) {
          ClassDependencies report = result.result();
          System.out.println("Package: " + report.getPackageName());
          System.out.println("Class: " + report.getClassName());
          System.out.println("getDependencies: " + report.getImportedDependencies());
          assertEquals("ClassDependencies", report.getClassName());
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
    Path path = Path.of(System.getProperty("user.dir"), "src/main/java/lib/");
    String normalizedPath = path.toString().replace("\\", "/");

    analyser.getPackageDependencies(Path.of(normalizedPath))
      .onComplete(result -> {
        if (result.succeeded()) {
          PackageDependencies report = result.result();
          System.out.println("Package: " + report.getPackageName());
          report.getClassReports().forEach(classDep -> {
            System.out.println("Class " + classDep.getClassName() + " depends on: " + classDep.getImportedDependencies());
          });
          vertxTestContext.completeNow();
        } else {
          result.cause().printStackTrace();
          vertxTestContext.failNow(result.cause());
        }
      });
  }

  @Test
  public void testProjectDependencies(Vertx vertx, VertxTestContext vertxTestContext) {
    DependencyAnalyserLib analyser = new DependencyAnalyserLib(vertx);
    Path path = Path.of(System.getProperty("user.dir"), "src/");
    String normalizedPath = path.toString().replace("\\", "/");

    analyser.getProjectDependencies(Path.of(normalizedPath))
      .onComplete(result -> {
        if (result.succeeded()) {
          ProjectDependencies report = result.result();
          report.getPackageReports().forEach(packageDep -> {
            System.out.println("Package " + packageDep.getPackageName());
            for (ClassDependencies classDep : packageDep.getClassReports()) {
              System.out.println("Class " + classDep.getClassName() + " depends on: " + classDep.getImportedDependencies());
            }
          });
          vertxTestContext.completeNow();
        } else {
          result.cause().printStackTrace();
          vertxTestContext.failNow(result.cause());
        }
      });
  }
}
