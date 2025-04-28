package lib;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lib.classes.*;
import lib.visitor.ClassVisitor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    vertx.fileSystem().readDir(String.valueOf(packageSrcFolder)).compose(results -> {
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
        promise.complete(new PackageDependencies("asd", classDependenciesSet));
      }
    ).onFailure(promise::fail);
    return promise.future();
  }

  public ProjectDependencies getProjectDependencies(String projectSrcFolder) {
    return null;
  }
}



