package AlgeNode;

import java.util.*;

public class AlgeNodeHelper {

    static Map<Integer,Integer> constructListQPSR (List<AlgeNode> inputs1, List<AlgeNode> inputs2,boolean isEq){
        if(inputs1.size()==inputs2.size()){
            Map<Integer,Integer> result = new HashMap<>();
            if (inputs1.size() >= 100){
               if (checkSimpleMatch(inputs1,inputs2,result,isEq)){
                   return result;
               }
            }else {
                if (checkInputMatch(inputs1, 0, inputs2, new HashSet<>(), result, isEq)) {
                    return result;
                }
            }
        }
        return null;
    }

    static private boolean checkSimpleMatch(List<AlgeNode> inputs1, List<AlgeNode> inputs2,Map<Integer,Integer> result,boolean isEq){
        for (int i = 0; i<inputs1.size();i++){
            if (isEq){
                if (inputs1.get(i).isEq(inputs2.get(i))){
                    result.put(i,i);
                }else{
                    return false;
                }
            }else{
                if (inputs1.get(i).constructQPSR(inputs2.get(i))){
                    result.put(i,i);
                }else{
                    return false;
                }
            }
        }
        return true;
    }

    static private boolean checkInputMatch(List<AlgeNode> inputs1, int index, List<AlgeNode> inputs2, Set<Integer> used, Map<Integer,Integer> inputMatches,boolean isEq){
        if(index<inputs1.size()){
            AlgeNode node1 = inputs1.get(index);
            for(int i=0;i<inputs2.size();i++){
                if (isEq){
                    if((!used.contains(i)) && node1.isEq(inputs2.get(i))){
                        inputMatches.put(index,i);
                        used.add(i);
                        return checkInputMatch(inputs1,index+1,inputs2,used,inputMatches,isEq);
                    }
                }else{
                    if((!used.contains(i)) && node1.constructQPSR(inputs2.get(i))){
                        inputMatches.put(index,i);
                        used.add(i);
                        return checkInputMatch(inputs1,index+1,inputs2,used,inputMatches,isEq);
                    }
                }
            }
            return false;
        }
        return true;
    }
}
