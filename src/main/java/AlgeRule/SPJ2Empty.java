package AlgeRule;

import AlgeNode.AlgeNode;
import AlgeNode.SPJNode;
import AlgeNode.EmptyNode;
import SymbolicRexNode.BoolPredicate;
import SymbolicRexNode.RexNodeUtility;
import SymbolicRexNode.RexNotIn;
import SymbolicRexNode.SymbolicColumn;
import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SPJ2Empty extends AlgeRuleBase{
    public SPJ2Empty (AlgeNode input) { this.input = input ;}

    @Override
    public boolean preCondition() {
        if (this.input instanceof SPJNode) {
            SPJNode spjNode = (SPJNode) this.input;
            List<BoolExpr> assign = new ArrayList<>();
            Set<RexNode> conditions = spjNode.getConditions();
            if(!conditions.isEmpty()) {
                Context z3Context = this.input.getZ3Context();
                List<SymbolicColumn> inputSymbolicColumns = inputSymbolicColumns(spjNode,z3Context);
                RexNodeUtility.reset();
                SymbolicColumn symbolicCondition = BoolPredicate.getAndNodeSymbolicColumn(conditions,inputSymbolicColumns,assign,z3Context);
                RexNodeUtility.reset();
                assign.add(symbolicCondition.isValueTrue());
                if(z3Utility.isUnsat(z3Utility.mkAnd(assign,z3Context),z3Context)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<SymbolicColumn> inputSymbolicColumns (SPJNode node,Context z3Context) {
        List<SymbolicColumn> inputColumns = new ArrayList<>();
        for (AlgeNode input : node.getInputs()){
            for (RexNode outputExpr : input.getOutputExpr()){
                inputColumns.add(SymbolicColumn.mkNewSymbolicColumn(z3Context,outputExpr));
            }
        }
        return inputColumns;
    }

    @Override
    public AlgeNode transformation() {
        return (new EmptyNode(this.input.getZ3Context()));
    }
}
