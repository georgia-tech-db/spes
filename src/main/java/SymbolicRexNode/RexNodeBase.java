package SymbolicRexNode;


import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;

public abstract class RexNodeBase {
    protected Context z3Context;
    protected SymbolicColumn output;
    protected List<SymbolicColumn> inputs;
    protected List<BoolExpr> assignConstraints;
    public RexNodeBase(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        this.inputs = inputs;
        this.z3Context = z3Context;
        this.assignConstraints = new ArrayList<>();
    }
    public SymbolicColumn getOutput(){
        return this.output;
    }
    public Expr getOutputValue(){
        return this.output.getSymbolicValue();
    }
    public BoolExpr getOutputNull(){
        return this.output.getSymbolicNull();
    }
    public List<BoolExpr> getAssignConstrains(){
        return this.assignConstraints;
    }
}
