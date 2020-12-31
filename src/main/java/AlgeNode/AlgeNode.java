package AlgeNode;

import RexNodeHelper.NotIn;
import SymbolicRexNode.SymbolicColumn;
import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;

import java.util.*;

abstract public class AlgeNode {
    protected Context z3Context;
    protected List<RelDataType> inputTypes;
    protected List<AlgeNode> inputs;
    protected List<RexNode> outputExpr;
    protected Set<RexNode> conditions;

    protected List<SymbolicColumn> symbolicColumns;
    protected SymbolicColumn symbolicCondition;

    protected BoolExpr variableConstraints;

    private boolean rewritable;


    protected void setBasicFields(Context z3Context, List<RelDataType> inputTypes, List<AlgeNode> inputs, List<RexNode> outputExpr, Set<RexNode> conditions){
        this.z3Context = z3Context;
        this.inputTypes = inputTypes;
        this.inputs = inputs;
        this.outputExpr = outputExpr;
        this.conditions = conditions;
        this.variableConstraints = z3Context.mkTrue();
        this.rewritable = true;
    }

    public Context getZ3Context() { return  this.z3Context; }

    // get basic output types
    public List<RelDataType> getInputTypes(){
        return this.inputTypes;
    }

    // get the algebraic nodes inputs
    public List<AlgeNode> getInputs(){
        return this.inputs;
    }


    public void setInputs(List<AlgeNode> inputs){
        this.inputs = inputs;
    }
    // set the output expr
    public void setOutputExpr(List<RexNode> outputExpr) {
        this.outputExpr = outputExpr;
    }

    // return the output expression
    public List<RexNode> getOutputExpr(){
        return this.outputExpr;
    }

    // return the set of filter conditions
    public Set<RexNode> getConditions(){
        return this.conditions;
    }

    //set the filter conditions
    public void setConditions(Set<RexNode> conditions){
        this.conditions = conditions;
    }

    // add one condition into the filter condition
    public void addCondition(RexNode condition) {
        this.conditions.add(condition);
    }

    public void addConditions(Collection<RexNode> conditions) {
        this.conditions.addAll(conditions);
    }

    // return the symbolic output tuple
    public List<SymbolicColumn> getSymbolicColumns(){
        return this.symbolicColumns;
    }

    // check if two nodes symbolic outputs are equivalent with default match
    public boolean checkSymbolicOutput(AlgeNode node){
        if(this == node){
            return true;
        }
        if(this.getOutputExpr().isEmpty() && node.getOutputExpr().isEmpty()){
            return true;
        }
        List<SymbolicColumn> symbolicOutputs1 = this.getSymbolicColumns();
        List<SymbolicColumn> symbolicOutputs2 = node.getSymbolicColumns();
        boolean result = z3Utility.symbolicOutputEqual(buildOutputEnv(node),symbolicOutputs1,symbolicOutputs2,z3Context);
        return result;
    }

    public boolean checkSymbolicOutput(AlgeNode node, Map<Integer,Integer> columnPairs){
        Map<Integer,Integer> simplifyColumnPairs = eliminateMatches(node,columnPairs);
        if(simplifyColumnPairs.isEmpty()){
            return true;
        }

        List<SymbolicColumn> symbolicTuple1 = new ArrayList<>();
        List<SymbolicColumn> symbolicTuple2 = new ArrayList<>();
        List<SymbolicColumn> symbolicOutputs1 = this.getSymbolicColumns();
        List<SymbolicColumn> symbolicOutputs2 = node.getSymbolicColumns();

        for(Map.Entry<Integer,Integer> columnPair:simplifyColumnPairs.entrySet()){
            symbolicTuple1.add(symbolicOutputs1.get(columnPair.getKey()));
            symbolicTuple2.add(symbolicOutputs2.get(columnPair.getValue()));
        }

        return z3Utility.symbolicOutputEqual(buildOutputEnv(node),symbolicTuple1,symbolicTuple2,z3Context);
    }

    private Map<Integer,Integer> eliminateMatches(AlgeNode node,Map<Integer,Integer> columnPairs){
        if(this == node) {
            Map<Integer, Integer> newColumnPairs = new HashMap<>();
            for (Map.Entry<Integer, Integer> columnPair : columnPairs.entrySet()) {
                int key = columnPair.getKey();
                int value = columnPair.getValue();
                if (key != value) {
                    newColumnPairs.put(key, value);
                }
            }
            return newColumnPairs;
        }else{
            return columnPairs;
        }
    }

    private BoolExpr buildOutputEnv(AlgeNode node){
        List<BoolExpr> env = new ArrayList<>();
        env.add(this.getVariableConstraints());
        env.add(node.getVariableConstraints());
        return z3Utility.mkAnd(env,z3Context);
    }

    public boolean isEq(AlgeNode node) {
        if(this.constructQPSR(node)){
            return checkSymbolicOutput(node);
        }
        return false;
    }

    public boolean isRewritable() {
        return rewritable;
    }

    public void disableRewrite(){
        this.rewritable = false;
    }
    public void enableRewrite(){
        this.rewritable = true;
    }

    public BoolExpr getVariableConstraints () {
        return this.variableConstraints;
    }

    public void setVariableConstraints () {
        List<BoolExpr> childrenAssign = new ArrayList<>();
        for (AlgeNode child : this.inputs){
            childrenAssign.add(child.variableConstraints);
        }
        this.variableConstraints = z3Utility.mkAnd(childrenAssign,z3Context);
    }

    abstract public boolean constructQPSR(AlgeNode node);

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("outputExpr: (");
        for (RexNode rexNode : outputExpr) {
            result.append(rexNode.toString());
        }
        result.append(")\nConditions : (");
        for(RexNode condition:conditions){
            if (condition instanceof NotIn){
                result.append(((NotIn) condition).print());
            }else {
                result.append("[").append(condition.toString()).append("] ,  ");
            }
        }
        result.append(")\n inputTables:\n");
        for (AlgeNode input : inputs) {
            result.append(input.toString()).append("\n");
        }
        return result.toString();
    }
    abstract public AlgeNode clone ();
}
