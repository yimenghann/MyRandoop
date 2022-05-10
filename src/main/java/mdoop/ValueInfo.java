package mdoop;

import com.github.javaparser.StaticJavaParser;

public class ValueInfo {

    public static boolean isReferenceType(String s) {
        if (s.equals("byte")) {
            return false;
        }
        if (s.equals("short")) {
            return false;
        }
        if (s.equals("int")) {
            return false;
        }
        if (s.equals("long")) {
            return false;
        }
        if (s.equals("float")) {
            return false;
        }
        if (s.equals("double")) {
            return false;
        }
        if (s.equals("char")) {
            return false;
        }
        if (s.equals("boolean")) {
            return false;
        }

        return true;
    }

    private String typeStr;

    private String nameBase;

    private int relativeIndex;

    public ValueInfo() {}

    public ValueInfo(String typeStr, String nameBase, int relativeIndex) {
        this.typeStr = typeStr;
        this.nameBase = nameBase;
        this.relativeIndex = relativeIndex;
    }

    public ValueInfo(ValueInfo info, int relativeIndex) {
        this.typeStr = info.getTypeStr();
        this.nameBase = info.getNameBase();
        this.relativeIndex = relativeIndex;
    }

    public String getTypeStr() {
        return typeStr;
    }

    public void setTypeStr(String typeStr) {
        this.typeStr = typeStr;
    }

    public int getRelativeIndex() {
        return relativeIndex;
    }

    public void setRelativeIndex(int relativeIndex) {
        this.relativeIndex = relativeIndex;
    }

    public String getNameBase() {
        return nameBase;
    }

    public void setNameBase(String nameBase) {
        this.nameBase = nameBase;
    }

    @Override
    public String toString() {
        String result = "{";
        result += "typeStr:" + typeStr + ", ";
        result += "nameBase:" + nameBase + ", ";
        result += "relativeIndex:" + relativeIndex + ", ";
        return result;
    }
}
