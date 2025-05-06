package lib.visitor;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lib.classes.ClassDependencies;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashSet;
import java.util.Set;

public class ClassVisitor extends VoidVisitorAdapter<Void> {
  private final Set<String> usedTypes = new HashSet<>();
  private String className = "UnknownClass";
  private String packageName = "UnknownPackage";

  @Override
  public void visit(ClassOrInterfaceType n, Void arg) {
    super.visit(n, arg);
    usedTypes.add(n.getNameAsString());
  }

  @Override
  public void visit(ClassOrInterfaceDeclaration n, Void arg) {
    super.visit(n, arg);
    className = n.getNameAsString();
  }

  @Override
  public void visit(ObjectCreationExpr n, Void arg) {
    super.visit(n, arg);
    n.getType().ifClassOrInterfaceType(t -> {
      usedTypes.add(t.getNameAsString());
    });
  }

  @Override
  public void visit(PackageDeclaration n, Void arg) {
    packageName = n.getNameAsString();
    super.visit(n, arg);
  }

  @Override
  public void visit(ImportDeclaration n, Void arg) {
    super.visit(n, arg);
    if (!n.isAsterisk()) {
      var typeName = n.getChildNodes().getFirst();
      usedTypes.add(typeName.toString());
    } else {
      var packageName = n.getChildNodes().getFirst();
      usedTypes.add(packageName.toString());
    }
  }

  public ClassDependencies getReport() {
    return new ClassDependencies(className, packageName, usedTypes);
  }
}
