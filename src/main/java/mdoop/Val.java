package mdoop;


import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.Expression;

public class Val {


    private Expression expression;

    public Val(ValueInfo info) {
        if (info.getNameBase().equals("const")) {
            expression = StaticJavaParser.parseExpression("null");
        } else {
            String reArr = info.getNameBase().replace("[]", "Arr");
            reArr += info.getRelativeIndex();
            expression = StaticJavaParser.parseExpression(reArr);
        }
        if (info.getTypeStr().equals("byte") && info.getNameBase().equals("const")) {
            expression = StaticJavaParser.parseExpression("1");
        }
        if (info.getTypeStr().equals("short") && info.getNameBase().equals("const")) {
            expression = StaticJavaParser.parseExpression("1");
        }
        if (info.getTypeStr().equals("int") && info.getNameBase().equals("const")) {
            expression = StaticJavaParser.parseExpression("1");
        }
        if (info.getTypeStr().equals("long") && info.getNameBase().equals("const")) {
            expression = StaticJavaParser.parseExpression("1");
        }
        if (info.getTypeStr().equals("float") && info.getNameBase().equals("const")) {
            expression = StaticJavaParser.parseExpression("1.0");
        }
        if (info.getTypeStr().equals("double") && info.getNameBase().equals("const")) {
            expression = StaticJavaParser.parseExpression("1.0");
        }
        if (info.getTypeStr().equals("char") && info.getNameBase().equals("const")) {
            CharLiteralExpr cp = new CharLiteralExpr('a');
            expression = cp;
        }
        if (info.getTypeStr().equals("boolean") && info.getNameBase().equals("const")) {
            expression = StaticJavaParser.parseExpression("true");
        }
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public static Object getObject(ValueInfo info) {
        if (info.getTypeStr().equals("byte") && info.getNameBase().equals("const")) {
            return (byte)1;
        }
        if (info.getTypeStr().equals("short") && info.getNameBase().equals("const")) {
            return (short)1;
        }
        if (info.getTypeStr().equals("int") && info.getNameBase().equals("const")) {
            return 1;
        }
        if (info.getTypeStr().equals("long") && info.getNameBase().equals("const")) {
            return 1l;
        }
        if (info.getTypeStr().equals("float") && info.getNameBase().equals("const")) {
            return 1.0f;
        }
        if (info.getTypeStr().equals("double") && info.getNameBase().equals("const")) {
           return 1.0d;
        }
        if (info.getTypeStr().equals("char") && info.getNameBase().equals("const")) {
            return 'a';
        }
        if (info.getTypeStr().equals("boolean") && info.getNameBase().equals("const")) {
            return true;
        }
        return null;
    }

    public static Class getClass(ValueInfo info) {
        if (info.getTypeStr().equals("byte") && info.getNameBase().equals("const")) {
            return byte.class;
        }
        if (info.getTypeStr().equals("short") && info.getNameBase().equals("const")) {
            return short.class;
        }
        if (info.getTypeStr().equals("int") && info.getNameBase().equals("const")) {
            return int.class;
        }
        if (info.getTypeStr().equals("long") && info.getNameBase().equals("const")) {
            return long.class;
        }
        if (info.getTypeStr().equals("float") && info.getNameBase().equals("const")) {
            return float.class;
        }
        if (info.getTypeStr().equals("double") && info.getNameBase().equals("const")) {
            return double.class;
        }
        if (info.getTypeStr().equals("char") && info.getNameBase().equals("const")) {
            return char.class;
        }
        if (info.getTypeStr().equals("boolean") && info.getNameBase().equals("const")) {
            return boolean.class;
        }
        return null;
    }
}
