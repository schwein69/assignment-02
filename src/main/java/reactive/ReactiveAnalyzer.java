package reactive;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lib.classes.ClassDependencies;

import java.nio.file.Path;

import java.util.Set;
import java.util.stream.Collectors;

public class ReactiveAnalyzer {
  public Observable<ClassDependencies> analyzeClassDependencies(Path srcJava) {
    return Observable.create(emitter -> {
      try {
        CompilationUnit cu = StaticJavaParser.parse(srcJava);
        // Get package name
        String packageName = cu.getPackageDeclaration()
          .map(pd -> pd.getName().asString())
          .orElse("");

        // Get class name (assume 1 top-level class per file)
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
