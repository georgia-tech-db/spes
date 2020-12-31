package SymbolicRexNode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;

import java.util.List;

public class CaseNode extends RexNodeBase {

    public CaseNode(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        this.output = SymbolicColumn.mkNewSymbolicColumn(z3Context,node);
        buildExpr(node);
    }
    private void buildExpr(RexNode node){
        RexCall callNode = (RexCall) node;
        List<RexNode> operands = callNode.getOperands();
        int count = 0;
        BoolExpr currentCondition = z3Context.mkFalse();
        BoolExpr[] possibleValue = new BoolExpr[(operands.size()+1)/2];
        while (count < operands.size()-1){
            RexNode condition = operands.get(count);
            RexNodeBase conditionConverter = RexNodeConverter.rexConstraints(inputs,condition,z3Context);
            this.assignConstraints.addAll(conditionConverter.getAssignConstrains());
            BoolExpr whenCondition = conditionConverter.getOutput().isValueTrue();
            BoolExpr chooseConstrains = z3Context.mkAnd(z3Context.mkNot(currentCondition),whenCondition);
            currentCondition = z3Context.mkOr(currentCondition,whenCondition);
            count++;
            RexNode value = operands.get(count);
            possibleValue[count/2] = getOutEqualValue(value,chooseConstrains);
            count++;

        }
        RexNode defaultValue = operands.get(count);
        possibleValue[count/2] = getOutEqualValue(defaultValue,z3Context.mkNot(currentCondition));
        this.assignConstraints.add((BoolExpr) z3Context.mkOr(possibleValue).simplify());
    }

    private BoolExpr getOutEqualValue(RexNode value, BoolExpr condition){
        RexNodeBase valueConstrains = RexNodeConverter.rexConstraints(inputs,value,z3Context);
        BoolExpr outputEqualValue = z3Context.mkEq(this.output.getSymbolicValue(),valueConstrains.getOutputValue());
        BoolExpr nullValue = z3Context.mkEq(this.output.getSymbolicNull(),valueConstrains.getOutputNull());

        this.assignConstraints.addAll(valueConstrains.getAssignConstrains());

        return z3Context.mkAnd(condition,outputEqualValue,nullValue);
    }
}
