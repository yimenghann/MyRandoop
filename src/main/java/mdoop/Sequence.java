package mdoop;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.List;

public class Sequence {

    private List<ScannedMethod> scannedMethodList = new ArrayList<>();

    //arguments value info
    private List<List<ValueInfo>> argInfoList = new ArrayList<>();

    //returned value info
    private List<ValueInfo> valueInfoList = new ArrayList<>();

    public List<List<ValueInfo>> getArgInfoList() {
        return argInfoList;
    }

    public void setArgInfoList(List<List<ValueInfo>> argInfoList) {
        this.argInfoList = argInfoList;
    }

    public List<ValueInfo> getValueInfoList() {
        return valueInfoList;
    }

    public void setValueInfoList(List<ValueInfo> valueInfoList) {
        this.valueInfoList = valueInfoList;
    }

    public void add(ScannedMethod scannedMethod, List<ValueInfo> args, ValueInfo value) {
        scannedMethodList.add(scannedMethod);
        argInfoList.add(args);
        valueInfoList.add(value);
    }


    public List<ScannedMethod> getScannedMethodList() {
        return scannedMethodList;
    }

    public void setScannedMethodList(List<ScannedMethod> scannedMethodList) {
        this.scannedMethodList = scannedMethodList;
    }

    public int getSize() {
        return scannedMethodList.size();
    }


    public List<Statement> reverseToStmtListWithInfo() {
        List<Statement> result = new ArrayList<>();
        for (int i = 0; i < scannedMethodList.size(); i++) {
            ScannedMethod m = scannedMethodList.get(i);
            List<Expression> arg = new ArrayList<>();
            for (ValueInfo info: argInfoList.get(i)) {
                Val val = new Val(info);
                arg.add(val.getExpression());
            }
            Val ret = new Val(valueInfoList.get(i));
            Expression retExp = ret.getExpression();

            if (m == null) {
                ValueInfo info = valueInfoList.get(i);
                String qualifiedName = info.getTypeStr();
                ExpressionStmt expressionStmt = TestCaseCreator.generateNullInstanceStmt(qualifiedName, retExp);
                result.add(expressionStmt);
            } else if (m.getCallableDeclaration().isConstructorDeclaration()) {
                ExpressionStmt expressionStmt = TestCaseCreator.generateConstructStmt(m, arg, retExp);
                result.add(expressionStmt);
            } else if (m.getCallableDeclaration().isMethodDeclaration()) {
                if (!m.getReturnType().toString().equals("void")) {
                    ExpressionStmt expressionStmt = TestCaseCreator.generateMethodCallStmt(m, arg, retExp);
                    result.add(expressionStmt);
                } else  {
                    // void methodcall must be the last method in the sequence, because other method are generated according to the extend method arg.
                    ExpressionStmt expressionStmt = TestCaseCreator.generateVoidMethodCallStmt(m, arg, retExp);
                    result.add(expressionStmt);
                }
            } else {
                System.out.println("a strange method neither ConstructorDeclaration and MethodDeclaration:");
                System.out.println(m.getCallableDeclaration());
            }
        }
        return result;
    }

    public List<Statement> reverseToStmtList() {
        return reverseToStmtListWithInfo();
    }

    public String toStringCache = null;

    public void figureToStingCache() {
        toStringCache = this.toString();
    }

    @Override
    public String toString() {
        List<Statement> statementList = reverseToStmtList();
        String result = "";
        for (Statement m : statementList) {
            result += m.toString() + "\n";
        }
        //System.out.println(result);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Sequence) {
            Sequence sequence = (Sequence) obj;
            return this.toString().equals(sequence.toString());
        } else {
            return false;
        }
    }
}
