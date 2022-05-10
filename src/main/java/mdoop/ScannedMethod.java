package mdoop;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.List;


public class ScannedMethod {

    private CallableDeclaration callableDeclaration;
    private String className;
    private String packageName;
    private String classQualifiedName;
    private ClassOrInterfaceType classOrInterfaceType;
    private List<String> resolvedParametersTypeStrList;

    public ScannedMethod(CallableDeclaration callableDeclaration) {
        this.callableDeclaration = callableDeclaration;
    }

    public CallableDeclaration getCallableDeclaration() {
        return callableDeclaration;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassQualifiedName() {
        return classQualifiedName;
    }

    public void setClassQualifiedName(String classQualifiedName) {
        this.classQualifiedName = classQualifiedName;
    }

    public ClassOrInterfaceType getClassOrInterfaceType() {
        return classOrInterfaceType;
    }

    public void setClassOrInterfaceType(ClassOrInterfaceType classOrInterfaceType) {
        this.classOrInterfaceType = classOrInterfaceType;
    }

    public List<String> getResolvedParametersTypeStrList() {
        return resolvedParametersTypeStrList;
    }

    public void setResolvedParametersTypeStrList(List<String> resolvedParametersTypeStrList) {
        this.resolvedParametersTypeStrList = resolvedParametersTypeStrList;
    }

    public Type getReturnType() {
        if (callableDeclaration.isConstructorDeclaration()) {
            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) callableDeclaration;
            constructorDeclaration.getName();
            Type type = StaticJavaParser.parseType(constructorDeclaration.getName().toString());
            return type;
        }
        if (callableDeclaration.isMethodDeclaration()) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) callableDeclaration;
            Type type = methodDeclaration.getType();
            return type;
        }
        return StaticJavaParser.parseType("void");
    }


    public String getResoleTypeName() {
        if (callableDeclaration.isConstructorDeclaration()) {
            return classQualifiedName;
        }
        if (callableDeclaration.isMethodDeclaration()) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) callableDeclaration;
            Type type = methodDeclaration.getType();
            String result = VoidVisitorComplete.FigureResolveTypeStr(type);
            return result;
        }
        return "xxx";
    }

    public SimpleName getName() {
        return callableDeclaration.getName();
    }

    public NodeList<Parameter> getParameters() {
        return callableDeclaration.getParameters();
    }

    public NodeList<Type> getParameterTypes() {
        NodeList<Parameter> parameters = callableDeclaration.getParameters();
        NodeList<Type> result = new NodeList<>();
        for (Parameter parameter : parameters) {
            result.add(parameter.getType());
        }
        return result;
    }


}
