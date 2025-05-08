package reactive;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.reactivex.rxjava3.core.Observable;
import lib.classes.ClassDependencies;

import java.nio.file.Path;

import java.util.Set;
import java.util.stream.Collectors;

public class ReactiveAnalyzer {
  public Observable<ClassDependencies> analyzeClassDependencies(Path srcJava) {
    return Observable.create(emitter -> {
      try {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);  // Java 14+ for record support
        StaticJavaParser.setConfiguration(configuration);  // Apply configuration

        CompilationUnit cu = StaticJavaParser.parse(srcJava);
        // Get package name
        String packageName = cu.getPackageDeclaration()
          .map(NodeWithName::getNameAsString)
          .orElse(srcJava.getParent().getFileName().toString());

        String className = cu.findFirst(ClassOrInterfaceDeclaration.class)
          .map(ClassOrInterfaceDeclaration::getNameAsString)
          .orElse(srcJava.getFileName().toString().replace(".java", ""));

        Set<String> dep = cu.findAll(ClassOrInterfaceType.class).stream()
          .map(ClassOrInterfaceType::getNameAsString)
          .collect(Collectors.toSet());
        dep.addAll(cu.findAll(ObjectCreationExpr.class).stream()
          .map(ObjectCreationExpr::getTypeAsString)
          .collect(Collectors.toSet()));

        emitter.onNext(new ClassDependencies(className, packageName, dep));
        emitter.onComplete();
      } catch (Exception e) {
        emitter.onError(e);
      }
    });
  }
}
