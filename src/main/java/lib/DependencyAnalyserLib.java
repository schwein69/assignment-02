package lib;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lib.classes.*;
import lib.visitor.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyAnalyserLib {
  private final Vertx vertx;

  public DependencyAnalyserLib(Vertx vertx) {
    this.vertx = vertx;
  }

  public Future<ClassDependencies> getClassDependencies(Path classSrcFile) {
    return
      vertx.fileSystem().readFile(classSrcFile.toString()).compose(buffer -> parseCompilationUnitAsync(buffer.toString("UTF-8"))).map(cu -> {
        ClassVisitor visitor = new ClassVisitor();
        visitor.visit(cu, null);
        return visitor.getReport();
      });
  }

  private Future<CompilationUnit> parseCompilationUnitAsync(String sourceCode) {
    return vertx.executeBlocking(() -> {
      ParserConfiguration config = new ParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
      StaticJavaParser.setConfiguration(config);
      return StaticJavaParser.parse(sourceCode);
    }, false);
  }

  public Future<PackageDependencies> getPackageDependencies(Path packageSrcFolder) {
    Promise<PackageDependencies> promise = Promise.promise();
    vertx.executeBlocking(() -> {
      try (Stream<Path> walk = Files.walk(packageSrcFolder)) {
        return walk
          .map(Path::toString)
          .filter(string -> string.endsWith(".java"))
          .collect(Collectors.toList());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).compose(results -> {
      List<Future<ClassDependencies>> futures = new ArrayList<>();
      for (String filePath : results) {
        if (filePath.endsWith(".java")) {
          futures.add(getClassDependencies(Path.of(filePath)));
        }
      }
      return Future.all(futures);
    }).onSuccess(cu -> {
        Set<ClassDependencies> classDependenciesSet = new HashSet<>();
        for (Object a : cu.result().list()) classDependenciesSet.add((ClassDependencies) a);

        // Dynamically determine the package name (use first available, or fallback)
        String packageName = classDependenciesSet.stream()
          .map(ClassDependencies::getPackageName)
          .filter(name -> name != null && !name.equals("UnknownPackage"))
          .map(name -> name.split("\\.")[0]) // Take top-level segment only
          .findFirst()
          .orElse("default");


      promise.complete(new PackageDependencies(packageName, classDependenciesSet));
      }
    ).onFailure(promise::fail);
    return promise.future();
  }


  public Future<ProjectDependencies> getProjectDependencies(Path projectSrcFolder) {
    Promise<ProjectDependencies> promise = Promise.promise();
    vertx.executeBlocking(() -> {
      try (Stream<Path> walk = Files.walk(projectSrcFolder)) {
        return walk
          .filter(p -> p.toString().endsWith(".java"))
          .collect(Collectors.groupingBy(path -> projectSrcFolder.relativize(path.getParent()).toString()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).compose(filesByPackage -> {
      List<Future<PackageDependencies>> packageDependenciesList = new ArrayList<>();

      for (Map.Entry<String, List<Path>> entry : filesByPackage.entrySet()) {
        String packageName = entry.getKey().replace(File.separatorChar, '.');
        List<Future<ClassDependencies>> classFuturesByPackage = entry.getValue().stream().map(this::getClassDependencies).toList();
        Future<PackageDependencies> packageFuture = Future.all(new ArrayList<>(classFuturesByPackage))
          .map(cf -> {
            Set<ClassDependencies> classDeps = new HashSet<>();
            for (int i = 0; i < cf.size(); i++) {
              classDeps.add(cf.resultAt(i));
            }
            return new PackageDependencies(packageName, classDeps);
          });

        packageDependenciesList.add(packageFuture);
      }
      return Future.all(packageDependenciesList);
    }).onSuccess(cu -> {
        Set<PackageDependencies> packageDependenciesSet = new HashSet<>();
        for (Object a : cu.result().list()) packageDependenciesSet.add((PackageDependencies) a);
        promise.complete(new ProjectDependencies(packageDependenciesSet));
      }
    ).onFailure(promise::fail);
    return promise.future();
  }
}



