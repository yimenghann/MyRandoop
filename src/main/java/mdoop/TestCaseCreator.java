package mdoop;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.VoidType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestCaseCreator {

    private ValuePool valuePool = new ValuePool();

    public static int index = 0; //position of the parameter in the sequences

    private Random rand = new Random();

    private List<ScannedMethod> scannedMethodList;

    public List<ScannedMethod> getScannedMethodList() {
        return scannedMethodList;
    }

    public void setScannedMethodList(List<ScannedMethod> scannedMethodList) {
        this.scannedMethodList = scannedMethodList;
    }


    public ValuePool getValuePool() {
        return valuePool;
    }

    public void setValuePool(ValuePool valuePool) {
        this.valuePool = valuePool;
    }



    //random pick a method / constructor
    public ScannedMethod randomPublicMethod() {
        ScannedMethod scannedMethod = scannedMethodList.get(rand.nextInt(scannedMethodList.size()));
        return scannedMethod;
    }


    //randomly choose from sequence pool and combine with the new method
    public List<ValueInfo> randomSeqsAndValsWithInfo(ScannedMethod m, List<Sequence> seqs) {
        List<ValueInfo> result = new ArrayList<>();
        List<String> resolvedTypes = new ArrayList<>(m.getResolvedParametersTypeStrList());

        int off = 0; //offset for parameterTypesList
        boolean isMethodButNotStatic = false;

        //check nonstatic method, change offset
        if (m.getCallableDeclaration().isMethodDeclaration()) {
            MethodDeclaration methodDeclaration = m.getCallableDeclaration().asMethodDeclaration();
            if (!methodDeclaration.isStatic()) {
                resolvedTypes.add(0, m.getClassQualifiedName()); //put classQualifiedName at index 0
                off = 1;
                isMethodButNotStatic = true;
            }
        }


        for (int i = 0; i < resolvedTypes.size(); i++) {
            String typeStr = resolvedTypes.get(i);
            if (!ValueInfo.isReferenceType(typeStr)) { //if primitive type, add same type value
                ValueInfo mInfo = new ValueInfo(typeStr,"const", -1);
                seqs.add(null);
                result.add(mInfo);
            } else { //if not primitive type
                Sequence sequence;
                if ((sequence = valuePool.findSequenceByTypeStr(typeStr)) != null) {
                    result.add(null);
                    seqs.add(sequence);
                    continue;
                } else {//if the typestr not found in the previous sequences
                    Sequence s = new Sequence();
                    ScannedMethod scannedMethod = null;
                    List<ValueInfo> arg = new ArrayList<>();

                    //get the type name of the target parameter
                    String originName;
                    if ((i == 0) && isMethodButNotStatic) { //classQualifiedName
                        originName = m.getName().toString();
                    } else {//get correspondence parameter type
                        originName = m.getParameterTypes().get(i-off).toString();
                    }

                    String nameBase = figureNameBaseFromOriginName(originName);
                    ValueInfo info = new ValueInfo(typeStr, nameBase, 0);
//                    System.out.println(info);
                    List list = new ArrayList();
                    list.add(info);
                    s.add(scannedMethod, arg, info); //add null, empty list, info
                    result.add(null);
                    seqs.add(s); //seqs add a null seq
                }
            }
        }
        return result;
    }

//    NodeList<Expression>
    private List<ValueInfo> figureArgsInfo(Sequence newSep, List<Sequence> seqs, List<ValueInfo> valueInfos) {
        List<ValueInfo> arg = new ArrayList<>();
        for (int i = 0; i < seqs.size(); i++) {
            Sequence sequence = seqs.get(i);
            if (sequence != null) {

                //put each method in the sequence into newSep, updating relativeIndex
                for (int j = 0; j < sequence.getSize(); j++) {

                    //get value info corresponds to a method in sequence
                    ScannedMethod scannedMethod = sequence.getScannedMethodList().get(j);
                    List<ValueInfo> methodArg = sequence.getArgInfoList().get(j);


                    List<ValueInfo> currArg = new ArrayList<>();
                    for (ValueInfo info: methodArg) {
                        ValueInfo mInof = new ValueInfo(info, info.getRelativeIndex() + index); //initial 0
                        currArg.add(mInof);
                    }
                    ValueInfo methodVal = sequence.getValueInfoList().get(j);
                    ValueInfo curVal = new ValueInfo(methodVal, methodVal.getRelativeIndex() + index);
                    newSep.add(scannedMethod, currArg, curVal);
                }
                List<ValueInfo> list = newSep.getValueInfoList();
                ValueInfo retInfo = list.get(list.size()-1); //get most recent added value info (of the sequence)
                ValueInfo argInfo = new ValueInfo(retInfo, retInfo.getRelativeIndex());
                arg.add(argInfo);
                index += sequence.getSize();

            } else { //primitive type input arg
                ValueInfo retInfo = valueInfos.get(i);
                arg.add(retInfo);
            }
        }
        return arg;
    }



    public static String figureNameBaseFromOriginName(String originName) {
        String nameBase;
        if (originName.contains("<")) {
            originName = originName.substring(0, originName.indexOf("<"));
        }

        if (originName.contains(".")) {
            originName = originName.substring(originName.lastIndexOf(".") + 1);
        }

        nameBase = originName.substring(0,1).toLowerCase() + originName.substring(1);
        return nameBase;
    }

    private static ValueInfo figureReturnValInfo(ScannedMethod m) {
//        ValueInfo valueInfo = new ValueInfo();
        String typeStr, nameBase;
//        int relativeIndex = index;
        if (m.getCallableDeclaration().isConstructorDeclaration()) {
            typeStr = m.getClassQualifiedName();
            String originName = m.getClassName();
            nameBase = originName.substring(0,1).toLowerCase() + originName.substring(1);

        } else if (m.getCallableDeclaration().isMethodDeclaration()) {
            if (!m.getReturnType().toString().equals("void")) {
                typeStr = m.getResoleTypeName();
                String originName = m.getReturnType().toString();
                nameBase = figureNameBaseFromOriginName(originName);
            } else {
                typeStr = "void";
                nameBase = "void";
            }
        } else {
            typeStr = "may error";
            nameBase = "may error";
        }
        ValueInfo result = new ValueInfo(typeStr, nameBase, index);
        return result;
    }


    public static ExpressionStmt generateConstructStmt(ScannedMethod m, List<Expression> arg, Expression res) {
        VariableDeclarator variableDeclarator = new VariableDeclarator();
        ObjectCreationExpr callee = new ObjectCreationExpr();
        ClassOrInterfaceType classOrInterfaceType = StaticJavaParser.parseClassOrInterfaceType(m.getClassQualifiedName());
        callee.setType(classOrInterfaceType);
        NodeList<Expression> argNodes = new NodeList<>(arg);
        callee.setArguments(argNodes);
        variableDeclarator.setInitializer(callee);
        variableDeclarator.setType(m.getClassQualifiedName());
        String instStr = res.toString();
        variableDeclarator.setName(instStr);
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
        ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
        return expressionStmt;
    }

    private static MethodCallExpr generateMethodCallExpr(ScannedMethod m, List<Expression> arg, Expression res) {
        Expression caller = null;
        int argBegin = 0;
        if (m.getCallableDeclaration().isStatic()) {
            caller = StaticJavaParser.parseExpression(m.getClassQualifiedName());
        } else {
            caller = arg.get(0);
            argBegin = 1;
        }
        NodeList<Expression> parArg = new NodeList<>();
        for (int i = argBegin; i < arg.size(); i++) {
            parArg.add(arg.get(i));
        }
        MethodCallExpr callee = new MethodCallExpr(caller, m.getName(), parArg);
        return callee;
    }

    public static ExpressionStmt generateVoidMethodCallStmt(ScannedMethod m, List<Expression> arg, Expression res) {

        MethodCallExpr callee = generateMethodCallExpr(m, arg, res);

        ExpressionStmt expressionStmt = new ExpressionStmt(callee);
        return expressionStmt;
    }

    public static ExpressionStmt generateMethodCallStmt(ScannedMethod m, List<Expression> arg, Expression res) {
        VariableDeclarator variableDeclarator = new VariableDeclarator();

        MethodCallExpr callee = generateMethodCallExpr(m, arg, res);

        variableDeclarator.setInitializer(callee);
        variableDeclarator.setType(m.getResoleTypeName());
        String instStr = res.toString();
        variableDeclarator.setName(instStr);
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
        ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
        return expressionStmt;
    }

    public static ExpressionStmt generateNullInstanceStmt(String qualifiedName, Expression res) {
        VariableDeclarator variableDeclarator = new VariableDeclarator();
        variableDeclarator.setType(qualifiedName);
        variableDeclarator.setName(res.toString());
        //System.out.println(res.toString());
        variableDeclarator.setInitializer("null");
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(variableDeclarator);
        ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
        return expressionStmt;
    }


    public Sequence extendWithVals(ScannedMethod m, List<Sequence> seqs, List<ValueInfo> valueInfos) {
        Sequence newSep = new Sequence();
        //add seqs to newseq
        List<ValueInfo> arg = figureArgsInfo(newSep, seqs, valueInfos);
        ValueInfo valueInfo = figureReturnValInfo(m);
        //add method  to new seq
        newSep.add(m, arg, valueInfo);

        return newSep;
    }

    public CompilationUnit generateCompilationUnit(List<MethodDeclaration> methodDeclarationList, String className, int number) {
        CompilationUnit compilationUnit = new CompilationUnit();
        compilationUnit.addImport("org.junit.FixMethodOrder");
        compilationUnit.addImport("org.junit.Test");
        compilationUnit.addImport("org.junit.runners.MethodSorters");
        ClassOrInterfaceDeclaration myClass = compilationUnit.addClass(className + number);
        SingleMemberAnnotationExpr singleMemberAnnotationExpr = new SingleMemberAnnotationExpr(new Name("FixMethodOrder"), new NameExpr("MethodSorters.NAME_ASCENDING"));
        myClass.addAnnotation(singleMemberAnnotationExpr);
        for (MethodDeclaration m: methodDeclarationList) {
            myClass.addMember(m);
        }
        return compilationUnit;
    }



    public MethodDeclaration generateCode(Sequence sequence, int num) {
        NodeList<Modifier> nodeList = new NodeList<>();
        nodeList.add(Modifier.publicModifier());
        VoidType voidType = new VoidType();
        MethodDeclaration methodDeclaration = new MethodDeclaration(nodeList, voidType, "test"+num);
        MarkerAnnotationExpr markerAnnotationExpr = new MarkerAnnotationExpr("Test");
        methodDeclaration.addAnnotation(markerAnnotationExpr);
        BlockStmt blockStmt = new BlockStmt();
        List<Statement> statements = sequence.reverseToStmtList();
        for (Statement s: statements) {
            blockStmt.addStatement(s);
        }

        if (statements.size() == 0) {
            System.out.println("statements size equals to 0:");
            System.out.println(sequence);
        }

        Expression e = statements.get(statements.size()-1).asExpressionStmt().getExpression();
        /*
            we only add assert on the sequence ends with a statement as "Class c = xxx" or "Class c = b.xxx"
            which means sequence like these will be skip: 1) return type is primitive 2) return type is void
         */
        if (e.isVariableDeclarationExpr()) {
            VariableDeclarator vd = e.asVariableDeclarationExpr().getVariable(0);
            if (!vd.getType().isPrimitiveType()) {
                Expression caller;
                NodeList<Expression> parArg;
                caller = StaticJavaParser.parseExpression(vd.getName().toString());
                parArg = new NodeList<>();
                MethodCallExpr toStringExpr = new MethodCallExpr(caller, "toString", parArg);
//            ExpressionStmt toStringStmt = new ExpressionStmt(toStringExpr);
                blockStmt.addStatement(toStringExpr);
                caller = StaticJavaParser.parseExpression(vd.getName().toString());
                parArg = new NodeList<>();
                MethodCallExpr hashCodeExpr = new MethodCallExpr(caller, "hashCode", parArg);
//            ExpressionStmt hashCodeStmt = new ExpressionStmt(hashCodeExpr);
                blockStmt.addStatement(hashCodeExpr);

                Expression inCaller =  StaticJavaParser.parseExpression(vd.getName().toString());
                NodeList<Expression> inParArg = new NodeList<>();
                inParArg.add(StaticJavaParser.parseExpression(vd.getName().toString()));
                MethodCallExpr inMethodCallExpr = new MethodCallExpr(inCaller, "equals", inParArg);
                caller = StaticJavaParser.parseExpression("org.junit.Assert");
                parArg = new NodeList<>();
                parArg.add(inMethodCallExpr);
                MethodCallExpr equalsToExpr = new MethodCallExpr(caller, "assertTrue", parArg);
                blockStmt.addStatement(equalsToExpr);

            }
        }
        NodeList<ReferenceType> throwableType = new NodeList<>();
        throwableType.add((ReferenceType) StaticJavaParser.parseType("Throwable"));
        methodDeclaration.setThrownExceptions(throwableType);

        methodDeclaration.setBody(blockStmt);
        return methodDeclaration;
    }
}
