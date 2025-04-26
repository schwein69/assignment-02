package lib;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lib.classes.*;
import lib.visitor.ClassVisitor;

import java.nio.file.Path;

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


  /*private Future<CompilationUnit> parseCompilationUnitAsync(String sourceCode) {
    return vertx.executeBlocking(() -> StaticJavaParser.parse(sourceCode), false);
  }*/
  private Future<CompilationUnit> parseCompilationUnitAsync(String sourceCode) {
    return vertx.executeBlocking(() -> {
      ParserConfiguration config = new ParserConfiguration()
        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
      StaticJavaParser.setConfiguration(config);
      return StaticJavaParser.parse(sourceCode);
    }, false);
  }

  public PackageDependencies getPackageDependencies(String packageSrcFolder) {
    return null;
  }

  public ProjectDependencies getProjectDependencies(String projectSrcFolder) {
    return null;
  }
}



