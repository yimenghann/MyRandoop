package mdoop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ValuePool {

    private Random random = new Random();

    public Map<String, List<Sequence>> map = new HashMap<>();

    //find a random sequence containing typeStr
    public Sequence findSequenceByTypeStr(String typeStr) {
        if (map.containsKey(typeStr)) {
            List<Sequence> sequenceList = map.get(typeStr);
            return sequenceList.get(random.nextInt(sequenceList.size()));
        }
        return null;
    }

    public void add(String typeStr, Sequence s) {
        if (!map.containsKey(typeStr)) {
            map.put(typeStr, new ArrayList<>());
        }
        map.get(typeStr).add(s);
    }

    public boolean contains(String typeStr, Sequence sequence) {
        if (!map.containsKey(typeStr)) {
            return false;
        }
        List<Sequence> list = map.get(typeStr);
        for (Sequence s: list) {
            if (s.equals(sequence)) {
                return true;
            }
        }
        return false;
    }

    public String getSize() {
        int outSize = map.keySet().size();
        int innerSize = 0;
        for (List<Sequence> list: map.values()) {
            innerSize += list.size();
        }
        return "{" + outSize + ":" + innerSize + "}";
    }
}
