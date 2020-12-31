package SymbolicRexNode;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import java.util.List;

public class ArithmeticExpr extends RexNodeBase {
    public ArithmeticExpr(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        buildExpr(node);
    }
    private void buildExpr(RexNode node){
        RexCall callNode = (RexCall) node;
        RexNode left = callNode.getOperands().get(0);
        RexNode right = callNode.getOperands().get(1);
        RexNodeBase leftConverter = RexNodeConverter.rexConstraints(inputs,left,z3Context);
        RexNodeBase rightConverter =RexNodeConverter.rexConstraints(inputs,right,z3Context);
        // build the output value based on the input value
        Expr value = convertArithmetic(leftConverter.getOutputValue(),rightConverter.getOutputValue(),callNode);
        // the output value is null if one of the input value is null
        BoolExpr isNull = (BoolExpr) z3Context.mkOr(leftConverter.getOutputNull(),rightConverter.getOutputNull()).simplify();
        this.assignConstraints.addAll(leftConverter.getAssignConstrains());
        this.assignConstraints.addAll(rightConverter.getAssignConstrains());

        this.output = new SymbolicColumn(value,isNull,z3Context);
    }
    private Expr convertArithmetic(Expr leftExpr, Expr rightExpr, RexCall callNode){
        SqlKind sqlKind = callNode.getKind();
        ArithExpr constraint1 = (ArithExpr)leftExpr;
        ArithExpr constraint2 = (ArithExpr)rightExpr;
        switch (sqlKind){
            case PLUS:
                return z3Context.mkAdd(constraint1,constraint2);
            case MINUS:
                return z3Context.mkSub(constraint1,constraint2);
            //TODO understand the semantic of divide, integer division, low or high round
            case DIVIDE:
                return z3Context.mkDiv(constraint1,constraint2);
            case TIMES:
                return z3Context.mkMul(constraint1,constraint2);
            default:{
                return null;
            }
        }
    }
}
