package mdoop;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    public static String outFolderPath;

    public static List<MethodDeclaration> methodDeclarationList = new ArrayList<>();
    public static List<MethodDeclaration> errorMethodDecList = new ArrayList<>();
    public static List<MethodDeclaration> extendMethodDecList = new ArrayList<>();

    public static List<Sequence> noErrSequenceList = new ArrayList<>();
    public static List<Sequence> errSequenceList = new ArrayList<>();
    public static List<Sequence> extendSequenceList = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        System.out.println("test");
        //input: input code path + output path + time limit
        if (args.length != 3) {
            System.out.println("lack Args: there should be 3 args");
            System.out.println("The first arg is the path of java codes folder which needs to generate test codes.");
            System.out.println("The second arg is the folder path where the test code will be written. (All the test code will be written in one ErrorTest file and one RegressionTest file)");
            System.out.println("The third arg is the time limit for execution");
            return;
        }

        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
        }

        VoidVisitorComplete.SRC_PATH = args[0];
        Main.outFolderPath = args[1];
        long timeLimit = Long.parseLong(args[2]) * 1000;

        long startTime = System.currentTimeMillis();

        TestCaseCreator testCaseCreator = new TestCaseCreator();

        //get all methods in the package
        List<ScannedMethod> scannedMethodList = VoidVisitorComplete.scanMethod();
        testCaseCreator.setScannedMethodList(scannedMethodList);
        TestCaseCreator.index = 0;

        Set<String> hasCreatedType = new HashSet<>();
        Set<ScannedMethod> hasCreatedScannedMethod = new HashSet<>();

        long workTime = 0;
        int num = 0;

        while (workTime < timeLimit) { //time limit 60s
            if (num % 1000 == 0) {
                System.out.println("workTime is " + workTime/1000 + "s");
            }
            boolean f = true;

            //randomly choose a qualified method (nonstatic / static with qualified class name)
            ScannedMethod nScannedMethod = testCaseCreator.randomPublicMethod();
            while (f) {
                if (nScannedMethod.getCallableDeclaration().isMethodDeclaration()) {
                    MethodDeclaration methodDeclaration = nScannedMethod.getCallableDeclaration().asMethodDeclaration();
                    if (!methodDeclaration.isStatic() && !hasCreatedType.contains(nScannedMethod.getClassQualifiedName())) {
                        nScannedMethod = testCaseCreator.randomPublicMethod();
                        continue;
                    }
                }
                f = false;
            }
            hasCreatedScannedMethod.add(nScannedMethod);
            hasCreatedType.add(nScannedMethod.getResoleTypeName());
            TestCaseCreator.index = 0;

            //create new sequence by concatenating sequences and method
            Sequence newSeq = doAExtend(testCaseCreator, nScannedMethod);

            //check exceptions and contract violations
            boolean[] exeResult;
            exeResult = executeAndAddToListAndPool(newSeq);

            newSeq.figureToStingCache();
            boolean violated = exeResult[0];
            boolean notFilterOut = exeResult[1]; //exceptions


            if (violated) {
                if (checkInculde(errSequenceList, newSeq)) {
                    errSequenceList.add(newSeq);
                }
            } else {
                if (checkInculde(noErrSequenceList, newSeq)) {
                    noErrSequenceList.add(newSeq);
                }
                if (notFilterOut) {
                    ValuePool valuePool = testCaseCreator.getValuePool();
                    if (!valuePool.contains(nScannedMethod.getResoleTypeName(), newSeq)) {
                        valuePool.add(nScannedMethod.getResoleTypeName(), newSeq);
                    }
                    if (checkInculde(extendSequenceList, newSeq)) {
                        //only for presentation
                        extendSequenceList.add(newSeq);
                    }
                }
            }
            workTime = System.currentTimeMillis() - startTime;
            num++;
        }

        for (int i = 0; i < noErrSequenceList.size(); i++) {
            Sequence s = noErrSequenceList.get(i);
            MethodDeclaration declaration = testCaseCreator.generateCode(s, i);
            methodDeclarationList.add(declaration);
        }
        for (int i = 0; i < errSequenceList.size(); i++) {
            Sequence s = errSequenceList.get(i);
            MethodDeclaration declaration = testCaseCreator.generateCode(s, i);
            errorMethodDecList.add(declaration);
        }


        System.out.println("pool size = " + testCaseCreator.getValuePool().getSize());
        System.out.println("methodDeclarationList size = " + methodDeclarationList.size());
        System.out.println("errorMethodDecList size = " + errorMethodDecList.size());

        CompilationUnit noErrorCp = testCaseCreator.generateCompilationUnit(methodDeclarationList, "RegressionTest",1);
        writeCodeToFile(noErrorCp, outFolderPath + "/RegressionTest1.java");
        CompilationUnit errorCp = testCaseCreator.generateCompilationUnit(errorMethodDecList,"ErrorTest" ,1);
        writeCodeToFile(errorCp, outFolderPath + "/ErrorTest1.java");

        System.out.println("extendSequenceList size = " + extendSequenceList.size());
        for (int i = 0; i < extendSequenceList.size(); i++) {
            Sequence s = extendSequenceList.get(i);
            MethodDeclaration declaration = testCaseCreator.generateCode(s, i);
            extendMethodDecList.add(declaration);
        }

        CompilationUnit extendCp = testCaseCreator.generateCompilationUnit(extendMethodDecList, "UseToExtendTest" ,1);
        writeCodeToFile(extendCp, outFolderPath + "/UseToExtendTest1.java");
    }

    public static boolean checkInculde(List<Sequence> sequenceList, Sequence newSeq) {
        boolean notInclude = true;
        for (Sequence s: sequenceList) {
            if (!s.toStringCache.equals(newSeq.toStringCache)) {
                continue;
            } else {
                notInclude = false;
                break;
            }
        }
        return notInclude;
    }

    public static Sequence doAExtend(TestCaseCreator testCaseCreator, ScannedMethod scannedMethod) {
        List<Sequence> seqs = new ArrayList<>();
        List<ValueInfo> valueInfos = testCaseCreator.randomSeqsAndValsWithInfo(scannedMethod, seqs);
        Sequence newSeq = testCaseCreator.extendWithVals(scannedMethod, seqs, valueInfos);
        return newSeq;
    }

    public static boolean[] executeAndAddToListAndPool(Sequence newSeq) {

        boolean flag = true;
        boolean violated = false;
        try {
            Executor.execute(newSeq);
        } catch (ClassNotFoundException e) {
            flag = false;
        } catch (NoSuchMethodException e) {
            flag = false;
        } catch (IllegalAccessException e) {
            flag = false;
        } catch (InvocationTargetException e) {
            flag = false;
        } catch (InstantiationException e) {
            flag = false;
        } catch (NullPointerException e) {
            flag = false;
        } catch (ContractException e) {
            flag = false;
            violated = true;
        }

        boolean[] result = new boolean[2];
        result[0] = violated; //error sequences
        result[1] = flag;
        return result;
    }

    public static void writeCodeToFile(CompilationUnit compilationUnit, String filePath) {
        File file = new File(filePath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(compilationUnit.toString().getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
