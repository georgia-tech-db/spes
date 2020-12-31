package AlgeNode;

import RexNodeHelper.RexNodeHelper;
import SymbolicRexNode.SymbolicColumn;
import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;

import java.util.*;

public class UnionNode extends AlgeNode{
    public UnionNode(List<AlgeNode> inputTables, Context z3Context, List<RelDataType> inputTypes){
        Set<RexNode> conditions = new HashSet<>();
        List<RexNode> outputExpr = new ArrayList<>();
        for(int i=0;i<inputTypes.size();i++){
            RexInputRef column = new RexInputRef(i,inputTypes.get(i));
            outputExpr.add(column);
        }
        this.setBasicFields(z3Context,inputTypes,inputTables,outputExpr,conditions);
    }
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Union Node: It has " + this.inputs.size() + " children" + "\n");
        result.append(")\n inputTables:\n");
        for (AlgeNode input : inputs) {
            result.append(input.toString()).append("\n");
        }
        return result.toString();
    }

    public boolean constructQPSR (AlgeNode node){
        if (node instanceof UnionNode){
            UnionNode unionNode = (UnionNode) node;
            Map<Integer,Integer> inputMatches = findEqMatches(unionNode);
            if (inputMatches != null){
                this.constructQPSR(unionNode);
                return true;
            }
        }
        return false;
    }

    public boolean constructQPSR1 (AlgeNode node){
        if (node instanceof UnionNode){
            UnionNode unionNode = (UnionNode) node;
            Map<Integer,Integer> inputMatches = findQPSRMatches(unionNode);
            if (inputMatches != null){
                List<BoolExpr> boolPredicates = new ArrayList<>();
                int count = 0;
                while (count<inputMatches.size()){
                    boolPredicates.add(z3Utility.mkBoolPredicate(z3Context));
                    count++;
                }
                List<BoolExpr> leftPredicates = new ArrayList<>();
                List<BoolExpr> rightPreicates = new ArrayList<>();
                for (Map.Entry<Integer,Integer> pairs : inputMatches.entrySet()){
                    leftPredicates.add(boolPredicates.get(pairs.getKey()));
                    rightPreicates.add(boolPredicates.get(pairs.getValue()));
                }
                this.constructSR(leftPredicates);
                unionNode.constructSR(rightPreicates);
                return true;
            }
        }
        return false;
    }

    private void constructSR(List<BoolExpr> boolPredicates) {
        List<RelDataType> outputTypes = new ArrayList<>();
        for (RexNode outputExpr : this.inputs.get(0).getOutputExpr()){
            outputTypes.add(outputExpr.getType());
        }
        this.symbolicColumns = SymbolicColumn.constructSymbolicTuple(outputTypes,z3Context);
        List<BoolExpr> columnsEqs = new ArrayList<>();
        List<BoolExpr> predicateRelations = new ArrayList<>();
        for (int i=0;i<this.inputs.size();i++){
            AlgeNode input = this.inputs.get(i);
            BoolExpr columnsEq = z3Utility.makeColumnsEq(this.symbolicColumns,input.getSymbolicColumns(),this.z3Context);
            columnsEqs.add(z3Context.mkImplies(boolPredicates.get(i),columnsEq));
            predicateRelations.add(exceptOneFalse(boolPredicates,i));
        }
        List<BoolExpr> variableConstraints = new ArrayList<>();
        BoolExpr orColumnsEqs = z3Utility.mkAnd(columnsEqs,z3Context);
        BoolExpr orFalse = z3Utility.mkAnd(predicateRelations,z3Context);
        BoolExpr onHolds = z3Utility.mkOr(boolPredicates,z3Context);
        variableConstraints.add(orColumnsEqs);
        variableConstraints.add(orFalse);
        variableConstraints.add(onHolds);
        this.setVariableConstraints();
        variableConstraints.add(this.variableConstraints);
        this.variableConstraints = z3Utility.mkAnd(variableConstraints,z3Context);
    }

    private BoolExpr exceptOneFalse(List<BoolExpr> boolPredicates, int index){
        if (boolPredicates.size() > 1) {
            BoolExpr truePredicate = boolPredicates.get(index);
            BoolExpr[] allFalse = new BoolExpr[boolPredicates.size() - 1];
            int count = 0;
            for (int i = 0; i < boolPredicates.size(); i++) {
                if (i != index){
                    allFalse[count] = z3Context.mkNot(boolPredicates.get(i));
                    count++;
                }
            }
            return z3Context.mkImplies(truePredicate,z3Context.mkAnd(allFalse));
        }else{
            return z3Context.mkTrue();
        }
    }

    private void constructQPSR (UnionNode unionNode){
        List<RelDataType> outputTypes = new ArrayList<>();
        for (RexNode outputExpr : this.inputs.get(0).getOutputExpr()){
            outputTypes.add(outputExpr.getType());
        }
        this.symbolicColumns = SymbolicColumn.constructSymbolicTuple(outputTypes,z3Context);
        unionNode.setSymbolicColumns(this.symbolicColumns);
        this.variableConstraintTrue();
        unionNode.variableConstraintTrue();
    }

    public void setSymbolicColumns(List<SymbolicColumn> symbolicTuple){
        this.symbolicColumns = symbolicTuple;
    }

    public void variableConstraintTrue () {
        this.variableConstraints = z3Context.mkTrue();
    }

    private Map<Integer,Integer> findEqMatches(UnionNode node){
        List<AlgeNode> inputs1 = this.getInputs();
        List<AlgeNode> inputs2 = node.getInputs();
        return AlgeNodeHelper.constructListQPSR(inputs1,inputs2,true);
    }

    private Map<Integer,Integer> findQPSRMatches(UnionNode node){
        List<AlgeNode> inputs1 = this.getInputs();
        List<AlgeNode> inputs2 = node.getInputs();
        return AlgeNodeHelper.constructListQPSR(inputs1,inputs2,false);
    }

    public AlgeNode clone () {
        List<AlgeNode> newInputs = new ArrayList<>();
        for (AlgeNode input : this.inputs){
            newInputs.add(input.clone());
        }
        List<RelDataType> columnTypes = new ArrayList<>();
        for (RexNode outputExpr : this.outputExpr){
            columnTypes.add(outputExpr.getType());
        }
        return (new UnionNode(newInputs,this.z3Context,columnTypes));
    }
}
