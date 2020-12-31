package SymbolicRexNode;

import com.microsoft.z3.Context;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;

import java.util.List;

public class RexInputRefConstraints extends RexNodeBase {
    public RexInputRefConstraints(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        buildExpr(node);

    }
    private void buildExpr(RexNode node){
        RexInputRef inputRef = (RexInputRef) node;
        int index = inputRef.getIndex();
        this.output = inputs.get(index);
    }
}
