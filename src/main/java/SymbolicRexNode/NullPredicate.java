package SymbolicRexNode;

import com.microsoft.z3.Context;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import java.util.List;

public class NullPredicate extends RexNodeBase {
    public NullPredicate(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        buildExpr(node);

    }
    private void buildExpr(RexNode node){
        RexCall callNode = (RexCall) node;
        SqlKind sqlKind = callNode.getKind();
        RexNode operand = callNode.getOperands().get(0);
        RexNodeBase rexNodeConverter = RexNodeConverter.rexConstraints(inputs,operand,z3Context);
        this.assignConstraints.addAll(rexNodeConverter.getAssignConstrains());
        if(sqlKind.equals(SqlKind.IS_NULL)){
            this.output = new SymbolicColumn(rexNodeConverter.getOutputNull(),z3Context.mkFalse(),z3Context);
        }
        if(sqlKind.equals(SqlKind.IS_NOT_NULL)){
           this.output = new SymbolicColumn(z3Context.mkNot(rexNodeConverter.getOutputNull()),z3Context.mkFalse(),z3Context);
        }

    }
}
