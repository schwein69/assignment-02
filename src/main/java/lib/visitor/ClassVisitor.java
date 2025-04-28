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
  private final Set<String> dependencies = new HashSet<>();
  private String className = "UnknownClass";
  private String packageName = "UnknownPackage";

  @Override
  public void visit(ClassOrInterfaceDeclaration n, Void arg) {
    super.visit(n, arg);
    className = n.getNameAsString();
    n.getExtendedTypes().forEach(type -> dependencies.add(type.getNameAsString()));
    n.getImplementedTypes().forEach(type -> dependencies.add(type.getNameAsString()));
  }

  @Override
  public void visit(FieldDeclaration n, Void arg) {
    super.visit(n, arg);
    System.out.println("Field decl: " + n.toString());
  }

  @Override
  public void visit(VariableDeclarator n, Void arg) {
    super.visit(n, arg);
    System.out.println("Variable decl: " + n.getType().asString());
    //dependencies.add(n.getType().asString());
  }

  @Override
  public void visit(ObjectCreationExpr n, Void arg) {
    super.visit(n, arg);
    System.out.println("Object creation: " + n.getType().getNameAsString());
    //dependencies.add(n.getType().getNameAsString());
  }

  @Override
  public void visit(MethodCallExpr n, Void arg) {
    //dependencies.add(n.getNameAsString());
    super.visit(n, arg);
    // System.out.println("MethodCallExpr: " + n.getNameAsString());
  }

  @Override
  public void visit(MethodDeclaration n, Void arg) {
    // Parameters
    n.getParameters().forEach(param -> {
      //dependencies.add(param.getNameAsString());
      System.out.println("Method decl + param: " + param.getType().asString());
    });
    // Return type
    if (n.getType() instanceof ClassOrInterfaceType type) {
      //dependencies.add(type.getNameAsString());
      System.out.println("return type: " + n.getType().asString() + " (method decl, return type)");
    }
    super.visit(n, arg);
  }

  @Override
  public void visit(PackageDeclaration n, Void arg) {
    packageName = n.getNameAsString();
    super.visit(n, arg);
  }

  @Override
  public void visit(MarkerAnnotationExpr n, Void arg) {
    super.visit(n, arg);
    //dependencies.add(n.getNameAsString());
    System.out.println("MarkerAnnotationExpr: " + n.getNameAsString());
  }

  @Override
  public void visit(ImportDeclaration n, Void arg) {
    super.visit(n, arg);
    if (!n.isAsterisk()) {
      var typeName = n.getChildNodes().getFirst();
      var packageName = typeName.getChildNodes().getFirst();
      String importString = "type " + typeName + " package: " + packageName + " (import)";
      System.out.println(importString);
      dependencies.add(importString);
    } else {
      var packageName = n.getChildNodes().getFirst();
      System.out.println("package " + packageName + " (import)");
      dependencies.add(packageName.toString());
    }
  }

  public ClassDependencies getReport() {
    return new ClassDependencies(className, packageName, dependencies);
  }
}
