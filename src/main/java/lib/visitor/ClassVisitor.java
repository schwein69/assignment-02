package lib.visitor;

import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lib.classes.ClassDependencies;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashSet;
import java.util.Set;

public class ClassVisitor extends VoidVisitorAdapter<Void> {
  private final Set<String> dependencies = new HashSet<>();
  private String className = "UnknownClass";
  private String packageName = "UnknownPackage";

  @Override
  public void visit(ClassOrInterfaceDeclaration n, Void arg) {
    className = n.getNameAsString();
    n.getExtendedTypes().forEach(type -> dependencies.add(type.getNameAsString()));
    n.getImplementedTypes().forEach(type -> dependencies.add(type.getNameAsString()));
    super.visit(n, arg);
  }


  @Override
  public void visit(VariableDeclarationExpr n, Void arg) {
    if (n.getElementType() instanceof ClassOrInterfaceType type) {
      System.out.println("Variable: " + type.getNameAsString());
      dependencies.add(type.getNameAsString());
    }
    super.visit(n, arg);
  }

  @Override
  public void visit(ObjectCreationExpr n, Void arg) {
    System.out.println("Class/interface: " + n.getType().getNameAsString());
    dependencies.add(n.getType().getNameAsString());
    super.visit(n, arg);
  }

  @Override
  public void visit(MethodCallExpr n, Void arg) {
    System.out.println("MethodCallExpr: " + n.getNameAsString());
    //dependencies.add(n.getNameAsString());
    super.visit(n, arg);
  }

  @Override
  public void visit(MethodDeclaration n, Void arg) {
    // Return type
    if (n.getType() instanceof ClassOrInterfaceType type) {
      dependencies.add(type.getNameAsString());
    }

    // Parameters
    n.getParameters().forEach(param -> {
      if (param.getType() instanceof ClassOrInterfaceType type) {
        dependencies.add(type.getNameAsString());
      }
    });

    super.visit(n, arg);
  }

  @Override
  public void visit(PackageDeclaration n, Void arg) {
    packageName = n.getNameAsString();
    super.visit(n, arg);
  }

  @Override
  public void visit(MarkerAnnotationExpr n, Void arg) {
    dependencies.add(n.getNameAsString());
    System.out.println("MarkerAnnotationExpr: " + n.getNameAsString());
    super.visit(n, arg);
  }

  public ClassDependencies getReport() {
    return new ClassDependencies(className, packageName, dependencies);
  }
}
