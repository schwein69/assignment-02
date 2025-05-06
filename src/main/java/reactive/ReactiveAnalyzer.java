package reactive;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.nio.file.Path;

import java.util.Set;
import java.util.stream.Collectors;

public class ReactiveAnalyzer {
  public Observable<String> analyzeClassDependencies(Path srcJava) {
    return Observable.create(emitter -> {
      try {
        CompilationUnit cu = StaticJavaParser.parse(srcJava);
        Set<String> dep = cu.findAll(ClassOrInterfaceType.class).stream()
          .map(ClassOrInterfaceType::getNameAsString)
          .collect(Collectors.toSet());
        dep.addAll(cu.findAll(ObjectCreationExpr.class).stream()
          .map(ObjectCreationExpr::getTypeAsString)
          .collect(Collectors.toSet()));
        dep.forEach(emitter::onNext);

        emitter.onComplete();
      } catch (Exception e) {
        emitter.onError(e);
      }
    });
  }
}
