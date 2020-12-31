package AlgeNode;

import SymbolicRexNode.SymbolicColumn;
import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import java.util.*;

public class AggNode extends AlgeNode{
    static public SqlKind[] distinctInsensitive = {SqlKind.MAX, SqlKind.MIN};
    static public SqlKind[] appendableAgg = {SqlKind.MAX, SqlKind.MIN,SqlKind.SUM,SqlKind.COUNT};
    private List<Integer> groupByList;
    private List<AggregateCall> aggregateCallList;
    public AggNode(List<Integer> groupByList, List<AggregateCall> aggregateCallList, List<RelDataType> inputTypes, AlgeNode  input, Context z3Context){
        List<AlgeNode> inputs = new ArrayList<>();
        inputs.add(input);
        List<RexNode> outputExpr = new ArrayList<>();
        for(int i=0;i<inputTypes.size();i++){
            RexInputRef column = new RexInputRef(i,inputTypes.get(i));
            outputExpr.add(column);
        }

        setBasicFields(z3Context,inputTypes,inputs,outputExpr,new HashSet<>());

        this.groupByList = groupByList;
        this.aggregateCallList = aggregateCallList;

    }

    public AlgeNode getInput(){
        return this.getInputs().get(0);
    }

    public void setAggregateCallList(List<AggregateCall> aggregateCallList){
        this.aggregateCallList = aggregateCallList;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Agg Node: \n Group By:(");
        for (Integer integer : groupByList) {
            result.append(integer).append(",");
        }
        result.append(")\n AggCallSet: (");
        for (AggregateCall aggregateCall : aggregateCallList) {
            result.append(aggregateCall.toString()).append(",");
        }
        result.append(")\n").append(super.toString());
        return result.toString();
    }

    public boolean constructQPSR (AlgeNode node) {
        if (node instanceof  AggNode) {
            AggNode aggNode = (AggNode) node;
            if (aggNode.getInput() instanceof EmptyNode) {
                if (this.getInput() instanceof EmptyNode){
                     this.emptyTableSymbolicColumn();
                     aggNode.emptyTableSymbolicColumn();
                     return true;
                }
            }
            if (this.isCartesianEq(aggNode)){
                List<SymbolicAggPair> symbolicAggPairs = new ArrayList<>();
                this.constructSymbolicColumns(symbolicAggPairs);
                aggNode.constructSymbolicColumns(symbolicAggPairs);
                return true;
            }
        }
        return false;
    }

    public void emptyTableSymbolicColumn(){
        this.symbolicColumns = new ArrayList<>();
        for (AggregateCall aggregateCall : this.aggregateCallList){
            if (aggregateCall.getAggregation().getKind().equals(SqlKind.COUNT)){
                SymbolicColumn value0 = new SymbolicColumn(z3Context.mkInt(0),z3Context.mkFalse(),z3Context);
                symbolicColumns.add(value0);
            }else{
                SymbolicColumn nullValue = new SymbolicColumn(z3Utility.mkDumpValue(aggregateCall.getType(),z3Context),z3Context.mkTrue(),z3Context);
                symbolicColumns.add(nullValue);
            }
        }
    }

    public boolean isCartesianEq(AggNode aggNode) {
        if (groupByEq(aggNode)) {
            return true;
        }
        return false;
    }

    private boolean groupByEq(AggNode node){
        AlgeNode input1 = this.getInput();
        AlgeNode input2 = node.getInput();
        if(input1.constructQPSR(input2)){
            this.setVariableConstraints();
            node.setVariableConstraints();
            return groupByColumnBijective(node);
        }
        return false;
    }

    public void constructSymbolicColumns(List<SymbolicAggPair> symbolicAggPairs){
        this.symbolicColumns = new ArrayList<>();
        this.symbolicColumns.addAll(this.getSymbolicGroupByColumns());
        this.symbolicColumns.addAll(this.constructSymbolicAggCalls(symbolicAggPairs));
    }

    private List<SymbolicColumn> constructSymbolicAggCalls(List<SymbolicAggPair> symbolicAggPairs){
        List<SymbolicColumn> symbolicAggCalls = new ArrayList<>();
        AlgeNode input = this.getInput();
        for (AggregateCall aggCall : this.aggregateCallList) {
            boolean findMatch = symbolicAggPairs.stream().anyMatch(
                    (SymbolicAggPair pair) -> {
                        if (pair.isEqualAggCall(input, aggCall)) {
                            symbolicAggCalls.add(pair.getSymbolicColumn());
                            return true;
                        } else {
                            return false;
                        }});
            if (!findMatch) {
                SymbolicColumn newColumn = SymbolicColumn.mkNewSymbolicColumn(z3Context, aggCall.getType());
                symbolicAggCalls.add(newColumn);
                symbolicAggPairs.add(new SymbolicAggPair(aggCall, newColumn, input));
            }
        }
        return symbolicAggCalls;
    }

    public List<Integer> getGroupByList(){
        return this.groupByList;
    }

    public List<RexNode> getGroupByVariables() {
        return this.outputExpr.subList(0,this.groupByList.size());
    }

    public List<AggregateCall> getAggregateCallList(){
        return this.aggregateCallList;
    }

    public List<SymbolicColumn> getSymbolicGroupByColumns(){
        List<SymbolicColumn> symbolicGroupByColumns = new ArrayList<>();
        for (int index : this.groupByList) {
            symbolicGroupByColumns.add(this.getInput().getSymbolicColumns().get(index));
        }
        return symbolicGroupByColumns;
    }

    private boolean groupByColumnBijective(AggNode node){
        List<SymbolicColumn> groupByColumn1 = this.getSymbolicGroupByColumns();
        List<SymbolicColumn> groupByColumn2 = node.getSymbolicGroupByColumns();
        if(groupByColumn1.isEmpty() && groupByColumn2.isEmpty()){
            return true;
        }
        BoolExpr env = constructFreshEnv(node);
        return groupBySymbolicDecide(env,groupByColumn1, groupByColumn2);

    }

    private boolean groupBySymbolicDecide(BoolExpr env, List<SymbolicColumn> groupByColumn1, List<SymbolicColumn> groupByColumn2) {
        BoolExpr eq1 = freshEq(groupByColumn1);
        BoolExpr eq2 = freshEq(groupByColumn2);
        BoolExpr Eq1DecideEq2 = z3Context.mkAnd(env,eq1,z3Context.mkNot(eq2));
        if(z3Utility.isUnsat(Eq1DecideEq2,z3Context)){
            BoolExpr Eq2DecideEq1 = z3Context.mkAnd(env,eq2,z3Context.mkNot(eq1));
            return z3Utility.isUnsat(Eq2DecideEq1,z3Context);
        }
        return false;
    }

    private BoolExpr freshEq(List<SymbolicColumn> groupByColumn){
        List<SymbolicColumn> freshColumn = SymbolicColumn.constructFreshColumns(groupByColumn,z3Context);
        BoolExpr[] columnEqs1 = new BoolExpr[groupByColumn.size()];
        for(int i=0;i<freshColumn.size();i++){
            columnEqs1[i] = z3Utility.symbolicColumnEq(groupByColumn.get(i),freshColumn.get(i),z3Context,false);
        }
        return z3Context.mkAnd(columnEqs1);
    }

    private BoolExpr constructFreshEnv(AggNode node){
        BoolExpr env1 = this.getVariableConstraints();
        BoolExpr freshEnv1 = (BoolExpr) z3Utility.constructFreshExpr(env1,z3Context);
        BoolExpr env2 = node.getVariableConstraints();
        BoolExpr freshEnv2 = (BoolExpr) z3Utility.constructFreshExpr(env2,z3Context);
        return z3Context.mkAnd(env1,freshEnv1,env2,freshEnv2);
    }

    public AlgeNode clone () {
        AlgeNode newInput = this.getInput().clone();
        List<RelDataType> columnTypes = new ArrayList<>();
        for (RexNode outputExpr : this.outputExpr){
            columnTypes.add(outputExpr.getType());
        }
        List<Integer> newGroupByList = new ArrayList<>();
        for (Integer value : this.groupByList){
            int index = value;
            newGroupByList.add(index);
        }
        List<AggregateCall> newAggregateCallList = new ArrayList<>();
        for (AggregateCall oldCall : this.aggregateCallList){
            List<Integer> newArgList = new ArrayList<>();
            for (Integer oldArg : oldCall.getArgList()){
                int arg = oldArg;
                newArgList.add(arg);
            }
            AggregateCall newCall = oldCall.copy(newArgList,oldCall.filterArg,oldCall.collation);
            newAggregateCallList.add(newCall);
        }
        return (new AggNode(newGroupByList,newAggregateCallList,columnTypes,newInput,this.z3Context));
    }
}