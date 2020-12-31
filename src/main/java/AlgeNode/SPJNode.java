package AlgeNode;

import RexNodeHelper.RexNodeHelper;
import SymbolicRexNode.*;
import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;

import java.util.*;

public class SPJNode extends AlgeNode{
    private List<SymbolicColumn> inputSymbolicColumns;

    public SPJNode(List<RexNode> outputExpr,Set<RexNode> conditions, List<AlgeNode> inputs, Context z3Context){
        List<RelDataType> types = new ArrayList<>();
        for(AlgeNode input: inputs){
            types.addAll(input.getInputTypes());
        }
        setBasicFields(z3Context,types,inputs,outputExpr,conditions);
    }

    @Override
    public String toString() {
        return "SPJ Node: " + "\n" + super.toString();
    }


    public boolean constructQPSR (AlgeNode node) {
        if (node instanceof SPJNode) {
            SPJNode spjNode = (SPJNode) node;
            if (this.isCartesianEq(spjNode)){
                RexNodeUtility.reset();
                this.constructSR();
                spjNode.constructSR();
                RexNodeUtility.reset();
                return true;
            }
        }
        return false;
    }

    public boolean isCartesianEq(SPJNode spjNode) {
             Map<Integer,Integer> inputMatches = checkInputMatch(spjNode);
             if(inputMatches!=null){
                 this.constructSymbolicColumns();
                 spjNode.constructSymbolicColumns();
                 this.setVariableConstraints();
                 spjNode.setVariableConstraints();
                 RexNodeUtility.reset();
                 boolean result = checkSymbolicCondition(spjNode);
                 RexNodeUtility.reset();
                 return result;
             }
             return false;
    }

    // construct the symbolic conditions based the symbolic inputs
    public SymbolicColumn constructSymbolicCondition(){
        List<BoolExpr> assign = new ArrayList<>();
        if(this.conditions.isEmpty()){
            this.symbolicCondition = new SymbolicColumn(z3Context.mkTrue(),z3Context.mkFalse(),z3Context);
        }else{
            this.symbolicCondition = BoolPredicate.getAndNodeSymbolicColumn(this.conditions,this.inputSymbolicColumns,assign,z3Context);
        }
        assign.add(this.variableConstraints);
        this.variableConstraints = z3Utility.mkAnd(assign,z3Context);
        return this.symbolicCondition;
    }

    /**
     * Check if the symbolic condition for this node is logically equivalent to that of another node.
     */
    private boolean checkSymbolicCondition(SPJNode node){
        SymbolicColumn condition1 = this.constructSymbolicCondition();
        SymbolicColumn condition2 = node.constructSymbolicCondition();
        List<BoolExpr> env = new ArrayList<>();
        env.add(this.variableConstraints);
        env.add(node.getVariableConstraints());
        if (z3Utility.isConditionEq(env, condition1.isValueTrue(), condition2.isValueTrue(), z3Context)){
            this.joinCondition(condition1.isValueTrue());
            node.joinCondition(condition2.isValueTrue());
            return true;
        }
        return false;
    }

    public void joinCondition(BoolExpr condition){
        this.variableConstraints = z3Context.mkAnd(this.variableConstraints,condition);
    }

    public void constructSR () {
        List<BoolExpr> assign = new ArrayList<>();
        this.symbolicColumns = new ArrayList<>();
        for (RexNode rexNode : this.outputExpr) {
            RexNodeBase converter = RexNodeConverter.rexConstraints(this.inputSymbolicColumns, rexNode, z3Context);
            this.symbolicColumns.add(converter.getOutput());
            assign.addAll(converter.getAssignConstrains());
        }
        assign.add(this.variableConstraints);
        this.variableConstraints = z3Utility.mkAnd(assign,z3Context);
    }

    public void constructSymbolicColumns () {
        this.inputSymbolicColumns = new ArrayList<>();
        for (AlgeNode input : this.inputs ){
            for (SymbolicColumn symbolicColumn : input.getSymbolicColumns()){
                this.inputSymbolicColumns.add(symbolicColumn);
            }
        }
    }

    private Map<Integer, Integer> checkInputMatch(SPJNode node){
        List<AlgeNode> inputs1 = this.inputs;
        List<AlgeNode> inputs2 = node.getInputs();
        return AlgeNodeHelper.constructListQPSR(inputs1,inputs2,false);
    }

    public AlgeNode clone () {
        List<AlgeNode> newInputs = new ArrayList<>();
        for (AlgeNode input : this.inputs){
            newInputs.add(input.clone());
        }
        List<RexNode> newInputExprs = new ArrayList<>();
        int offSize = 0;
        for (AlgeNode input : newInputs){
            for (RexNode inputExpr : input.getOutputExpr()){
                newInputExprs.add(RexNodeHelper.addOffSize(inputExpr,offSize));
            }
            offSize = offSize+input.getOutputExpr().size();
        }
        List<RexNode> newOutputExprs = new ArrayList<>();
        for (RexNode oldOutputExpr : this.outputExpr){
            newOutputExprs.add(RexNodeHelper.substitute(oldOutputExpr,newInputExprs));
        }
        Set<RexNode> newConditions = new HashSet<>();
        for (RexNode oldCondition : this.conditions){
            newConditions.add(RexNodeHelper.substitute(oldCondition,newInputExprs));
        }
        return (new SPJNode(newOutputExprs,newConditions,newInputs,this.z3Context));

    }
}
