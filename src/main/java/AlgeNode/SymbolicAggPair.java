package AlgeNode;

import SymbolicRexNode.SymbolicColumn;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlKind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolicAggPair {

    private AggregateCall aggCall;
    private SymbolicColumn symbolicColumn;
    private AlgeNode input;

    public SymbolicAggPair(AggregateCall aggCall, SymbolicColumn symbolicColumn, AlgeNode input) {
        this.aggCall = aggCall;
        this.symbolicColumn = symbolicColumn;
        this.input = input;
    }

    public boolean isEqualAggCall(AlgeNode cpInput, AggregateCall aggCall2) {
        if(isCallEqual(this.aggCall,aggCall2)){
            List<Integer> args1 = this.aggCall.getArgList();
            List<Integer> args2 = aggCall2.getArgList();
            if(args1.size()==args2.size()) {
                return input.checkSymbolicOutput(cpInput, constructColumnPairs(args1, args2));
            }
        }
        return false;
    }

    public SymbolicColumn getSymbolicColumn() {
        return symbolicColumn;
    }

    private Map<Integer, Integer> constructColumnPairs(List<Integer> args1, List<Integer> args2) {
        Map<Integer,Integer> columnPairs = new HashMap<>();
        for(int i=0;i<args1.size();i++){
            columnPairs.put(args1.get(i),args2.get(i));
        }
        return columnPairs;
    }

    private boolean isCallEqual(AggregateCall call1,AggregateCall call2) {
        SqlAggFunction aggFunction1 = call1.getAggregation();
        SqlAggFunction aggFunction2 = call2.getAggregation();
        SqlKind kind = aggFunction1.getKind();
        if (aggFunction1.getKind().equals(aggFunction2.getKind())){
            if (call1.isApproximate() == call2.isApproximate()){
                if (isDistinctSensitive(kind)) {
                    return call1.isDistinct()==call2.isDistinct();
                } else {
                    return true;
                }
            }
        }
        return false;
    }
    static private boolean isDistinctSensitive(SqlKind kind) {
        for (SqlKind sqlKind : AggNode.distinctInsensitive) {
            if (kind.equals(sqlKind)) {
                return false;
            }
        }
        return true;
    }
}
