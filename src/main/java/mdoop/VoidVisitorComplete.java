package mdoop;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VoidVisitorComplete {

        public static String SRC_PATH;

    public static List<ScannedMethod> scanMethod() {

        //Resolve classes from the JRE that is currently running.
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        //Resolve classes in the path
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(SRC_PATH));

        CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(reflectionTypeSolver);
        combinedSolver.add(javaParserTypeSolver);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);

        Path pathToSource = Paths.get(SRC_PATH);
        SourceRoot sourceRoot = new SourceRoot(pathToSource);
        sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);
        try {
            sourceRoot.tryToParse();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //get all files in the package as compilation units
        List<CompilationUnit> compilations = sourceRoot.getCompilationUnits();

        List<ScannedMethod> scannedMethodList = new ArrayList<>();

        for (CompilationUnit cu : compilations) {

            PackageDeclaration packageDeclaration = cu.getPackageDeclaration().get();
            String packageName = packageDeclaration.getNameAsString();

            //collector visit all class and interface in the file
            List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations = new ArrayList<>();
            VoidVisitorAdapter<List<ClassOrInterfaceDeclaration>> classOrInterfaceDeclarationCollector = new ClassOrInterfaceDeclarationCollector();
            classOrInterfaceDeclarationCollector.visit(cu, classOrInterfaceDeclarations);



            for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration : classOrInterfaceDeclarations) {
                if (!classOrInterfaceDeclaration.isPublic()) {
                    continue;
                }
                //visit all methods in the class or interface
                List<MethodDeclaration> methodDeclarations = new ArrayList<>();
                VoidVisitorAdapter<List<MethodDeclaration>> methodDeclarationCollector = new MethodDeclarationCollector();
                methodDeclarationCollector.visit(classOrInterfaceDeclaration, methodDeclarations);

                //visit constructors in the class or interface
                List<ConstructorDeclaration> constructorDeclarations = new ArrayList<>();
                VoidVisitorAdapter<List<ConstructorDeclaration>> constructorDeclarationCollector = new ConstructorDeclarationCollector();
                constructorDeclarationCollector.visit(classOrInterfaceDeclaration, constructorDeclarations);

                //put all method info into scannedMethodList
                for (MethodDeclaration methodDeclaration : methodDeclarations) {
                    String classQualifiedName = classOrInterfaceDeclaration.resolve().getQualifiedName();
                    if (!checkIfDirectMember(methodDeclaration, classQualifiedName)) {
                        continue;
                    }
                    if (!methodDeclaration.isPublic()) {
                        continue;
                    }
                    //get method input parameters
                    NodeList<Parameter> parameters = methodDeclaration.getParameters();
                    //all types of parameters input in the method
                    List<String> resolvedParametersTypeStrList = new ArrayList<>();

                    for (Parameter parameter : parameters) {
                        Type type = parameter.getType();
                        String resolvedTypeStr = FigureResolveTypeStr(type);
                        resolvedParametersTypeStrList.add(resolvedTypeStr);
                    }

                    ScannedMethod scannedMethod = new ScannedMethod(methodDeclaration);
                    scannedMethod.setClassName(classOrInterfaceDeclaration.getNameAsString());
                    scannedMethod.setClassQualifiedName(classQualifiedName);
                    scannedMethod.setResolvedParametersTypeStrList(resolvedParametersTypeStrList);
                    scannedMethodList.add(scannedMethod);
                }

                //put all constructor info into scannedMethodList
                for (ConstructorDeclaration constructorDeclaration : constructorDeclarations) {
                    String classQualifiedName = classOrInterfaceDeclaration.resolve().getQualifiedName();
                    if (classOrInterfaceDeclaration.isAbstract()) {
                        continue;
                    }
                    if (!constructorDeclaration.isPublic()) {
                        continue;
                    }
                    if (!checkIfDirectMember(constructorDeclaration, classQualifiedName)) {
                        continue;
                    }

                    NodeList<Parameter> parameters = constructorDeclaration.getParameters();
                    List<String> resolvedParametersTypeStrList = new ArrayList<>();
                    String info = "";
                    for (Parameter parameter : parameters) {
                        Type type = parameter.getType();
                        String resolvedTypeStr = FigureResolveTypeStr(type);
                        resolvedParametersTypeStrList.add(resolvedTypeStr);
                        info += resolvedTypeStr + ", ";
                    }
                    info = "(" + info + ")";

                    //System.out.println(info);
                    ScannedMethod scannedMethod = new ScannedMethod(constructorDeclaration);
                    scannedMethod.setClassName(classOrInterfaceDeclaration.getNameAsString());
                    scannedMethod.setPackageName(packageName);
                    scannedMethod.setClassQualifiedName(classQualifiedName);
                    scannedMethod.setResolvedParametersTypeStrList(resolvedParametersTypeStrList);
                    scannedMethodList.add(scannedMethod);
                }
            }
        }
        return scannedMethodList;
    }

    public static String getTypeArg(Type type) {
        String info = "";
        if (type instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType classOrInterfaceType = type.asClassOrInterfaceType();
            Optional<NodeList<Type>> typeArguments = classOrInterfaceType.getTypeArguments();
            if (typeArguments.isPresent()) {
                NodeList<Type> tArgs = typeArguments.get();
                for (int i = 0; i < tArgs.size(); i++) {
                    Type t = tArgs.get(i);
                    if (i == 0) {
                        info += FigureResolveTypeStr(t);
                    } else {
                        info += ", " + FigureResolveTypeStr(t);
                    }
                }
                info = "<" + info + ">";
            }
        }
        return info;
    }

    //resolve type string
    public static String FigureResolveTypeStr(Type type) {
        ResolvedType resolvedType = type.resolve();
        if (resolvedType.isPrimitive()) {
            return type.toString();
        } else if (resolvedType.isReferenceType()) {
            String info = getTypeArg(type);
            String resolvedTypeStr = resolvedType.asReferenceType().getQualifiedName() + info;
            return resolvedTypeStr;
        } else if (resolvedType.isArray()) {
            ArrayType a = type.asArrayType();
            Type eleType = a.getElementType();
            int depth = a.getArrayLevel();
            String eleResStr = FigureResolveTypeStr(eleType);
            String res = eleResStr;
            for (int i = 0; i < depth; i++) {
                res += "[]";
            }
            return res;
        }
        return "xxx";
    }

    //methodDeclaration
    public static boolean checkIfDirectMember(CallableDeclaration callableDeclaration, String classQualifiedName) {
        //check method has parent node
        if (!callableDeclaration.hasParentNode()) {
            return false;
        }

        //check equal class name
        Node a = callableDeclaration.getParentNode().get();
        if (!(a instanceof ClassOrInterfaceDeclaration)) {
            return false;
        }

        //check equal qualified name
        ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) a;
        String parentName = c.resolve().getQualifiedName();
        if (!parentName.equals(classQualifiedName)) {
            return false;
        }
        return true;
    }


    private static class MethodDeclarationCollector extends VoidVisitorAdapter<List<MethodDeclaration>> {
        @Override
        public void visit(MethodDeclaration md, List<MethodDeclaration> collector) {
            super.visit(md, collector);
            if (md.isPublic()) {
                collector.add(md);
            }
        }
    }

    private static class ConstructorDeclarationCollector extends VoidVisitorAdapter<List<ConstructorDeclaration>> {
        @Override
        public void visit(ConstructorDeclaration cd, List<ConstructorDeclaration> collector) {
            super.visit(cd, collector);
            collector.add(cd);
        }
    }

    private static class ClassOrInterfaceDeclarationCollector extends VoidVisitorAdapter<List<ClassOrInterfaceDeclaration>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration cid, List<ClassOrInterfaceDeclaration> collector) {
            super.visit(cid, collector);
            collector.add(cid);
        }
    }

}
