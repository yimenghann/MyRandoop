package mdoop;

import com.github.javaparser.ast.expr.Expression;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Executor {

    public static Class[] figureArgClassArr(List<ValueInfo> argList, int argBegin) throws ClassNotFoundException {
        Class[] result;
        List<Class> argClassList = new ArrayList<>();
        for (int i = argBegin; i < argList.size(); i++) {
            ValueInfo info = argList.get(i);
            if (Val.getClass(info) != null) { //primitive
                argClassList.add(Val.getClass(info));
            } else { //reference class
                argClassList.add(Class.forName(info.getTypeStr()));
            }
        }
        result = argClassList.toArray(new Class[argClassList.size()]);
        return result;
    }

    public static Object[] figureArgObjectArr(List<ValueInfo> argList, int argBegin, Map<String, Object> map) {
        Object[] result;
        List<Object> argObjectList = new ArrayList<>();
        for (int i = argBegin; i < argList.size(); i++) {
            ValueInfo info = argList.get(i);
            Val val = new Val(info);
            String a = val.getExpression().toString();
            if (map.containsKey(a)) {
                argObjectList.add(map.get(a));
            } else {
                argObjectList.add(Val.getObject(info));
            }
        }
        result = argObjectList.toArray();
        return result;
    }

    public static void execute(Sequence sequence) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ContractException {
        List<ScannedMethod> scannedMethodList = sequence.getScannedMethodList();
        List<List<ValueInfo>> argInfoList = sequence.getArgInfoList();
        List<ValueInfo> valueInfoList = sequence.getValueInfoList();
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < scannedMethodList.size(); i++) {

            ScannedMethod sm = scannedMethodList.get(i);
            List<ValueInfo> argList = argInfoList.get(i);
            ValueInfo ret = valueInfoList.get(i);

            Val retVal = new Val(ret);
            Expression retExp = retVal.getExpression();
            if (sm == null) {
                map.put(retExp.toString(), null);
            } else if (sm.getCallableDeclaration().isConstructorDeclaration()) {
//                System.out.println(sm.getClassQualifiedName());
                Class c = Class.forName(sm.getClassQualifiedName());
                Class[] argClassArr = figureArgClassArr(argList, 0);
                Object[] argObjectArr = figureArgObjectArr(argList, 0, map);
                Constructor<?> con = c.getConstructor(argClassArr);
                Object o = con.newInstance(argObjectArr);
                map.put(retExp.toString(), o);
            } else if (sm.getCallableDeclaration().isMethodDeclaration()) {
                Class c = Class.forName(sm.getClassQualifiedName());
                String callerStr;
                Object caller;
                int argBegin = 0;
                if (sm.getCallableDeclaration().isStatic()) {
                    caller = null;
                } else {
                    Val callerVal = new Val(argList.get(0));
                    callerStr = callerVal.toString();
                    caller = map.get(callerStr);
                    argBegin = 1;
                }
                Class[] argClassArr = figureArgClassArr(argList, argBegin);
                Object[] argObjectArr = figureArgObjectArr(argList, argBegin, map);
                Method m = c.getMethod(sm.getName().toString(), argClassArr);
                Object o = m.invoke(caller, argObjectArr);
                if (!sm.getReturnType().toString().equals("void")) {
                    map.put(retExp.toString(), o);
                } else {

                }
            } else {
                System.out.println("a strange method neither ConstructorDeclaration and MethodDeclaration:");
                System.out.println(sm.getCallableDeclaration());
            }
        }

        ValueInfo lastRetInfo = valueInfoList.get(valueInfoList.size()-1);
        Val val = new Val(lastRetInfo);
        Object o;
        String a = val.getExpression().toString();
        String typeStr = lastRetInfo.getTypeStr();
        if (!typeStr.equals("void") && ValueInfo.isReferenceType(typeStr)) {
            o = map.get(a);
            checkContracts(o);
        }
    }

    public static boolean checkContracts(Object o) throws ContractException {
        boolean flag = true;
        try {
            if (o == null) {
                flag = false;
            }
            if (o.toString() == null) {
                flag = false;
            }
            if (o.equals(o) == false) {
                flag = false;
                throw new ContractException();
            }
            o.hashCode();
        } catch (Exception e) {
//            e.printStackTrace();
            ContractException ce = new ContractException();
            throw ce;
        }
        return flag;
    }

}
