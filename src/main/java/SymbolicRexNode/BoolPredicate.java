package SymbolicRexNode;

import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BoolPredicate extends RexNodeBase {

    public BoolPredicate(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        buildExpr(node);
    }
    private void buildExpr(RexNode node){
        RexCall callNode = (RexCall) node;
        SqlKind sqlKind = node.getKind();
        switch (sqlKind){
            case NOT: {
                buildNotExpr(callNode);
                break;
            }
            case AND: {
                List<RexNode> operands = callNode.getOperands();
                this.output = getAndNodeSymbolicColumn(operands,inputs,assignConstraints,z3Context);
                break;
            }
            case OR: {
                List<RexNode> operands = callNode.getOperands();
                this.output = getOrNodeSymbolicColumn(operands,inputs,assignConstraints,z3Context);
                break;
            }
            /*
            case IN: {
                buildINExpr(callNode);
                break;
            }*/
            default:{
                break;
                }
        }
    }
    private void buildNotExpr(RexCall node){
        RexNode operand = node.getOperands().get(0);
        RexNodeBase converter = RexNodeConverter.rexConstraints(inputs,operand,z3Context);
        this.assignConstraints.addAll(converter.getAssignConstrains());
        //based on the sql standard to decide the relations of output value between input value
        this.output = new SymbolicColumn(z3Context.mkNot((BoolExpr)converter.getOutputValue()).simplify(),converter.getOutputNull(),z3Context);
    }
    static public SymbolicColumn getAndNodeSymbolicColumn(Collection<RexNode> operands, List<SymbolicColumn> inputs, List<BoolExpr> assignConstraints, Context z3Context){
        List<BoolExpr> resultValues = new ArrayList<>();
        List<BoolExpr> nullValues = new ArrayList<>();
        List<BoolExpr> nullSymbol = new ArrayList<>();
        for(RexNode operand:operands){
            RexNodeBase converter = RexNodeConverter.rexConstraints(inputs,operand,z3Context);
            BoolExpr result = (BoolExpr)converter.getOutputValue();
            BoolExpr symbolicNull = converter.getOutputNull();
            resultValues.add(result);
            nullValues.add(z3Context.mkOr(symbolicNull,result));
            nullSymbol.add(symbolicNull);
            assignConstraints.addAll(converter.getAssignConstrains());
        }
        BoolExpr outputValue = z3Utility.mkAnd(resultValues,z3Context);
        BoolExpr outputNull = (BoolExpr) z3Context.mkAnd(z3Utility.mkAnd(nullValues,z3Context),z3Utility.mkOr(nullSymbol,z3Context)).simplify();
        return (new SymbolicColumn(outputValue,outputNull,z3Context));
    }
    static public SymbolicColumn getOrNodeSymbolicColumn(Collection<RexNode> operands, List<SymbolicColumn> inputs, List<BoolExpr> assignConstraints, Context z3Context){
        List<BoolExpr> resultValues = new ArrayList<>();
        List<BoolExpr> nullValues = new ArrayList<>();
        List<BoolExpr> nullSymbol = new ArrayList<>();
        for(RexNode operand:operands){
            RexNodeBase converter = RexNodeConverter.rexConstraints(inputs,operand,z3Context);
            BoolExpr result = (BoolExpr)converter.getOutputValue();
            BoolExpr symbolicNull = converter.getOutputNull();
            resultValues.add(result);
            nullValues.add(z3Context.mkOr(symbolicNull,z3Context.mkNot(result)));
            nullSymbol.add(symbolicNull);

            assignConstraints.addAll(converter.getAssignConstrains());
        }
        BoolExpr outputValue = z3Utility.mkOr(resultValues,z3Context);
        BoolExpr outputNull = (BoolExpr) z3Context.mkAnd(z3Utility.mkAnd(nullValues,z3Context),z3Utility.mkOr(nullSymbol,z3Context)).simplify();
        return (new SymbolicColumn(outputValue,outputNull,z3Context));

    }

    private void buildINExpr(RexCall node){
        List<RexNode> operands = node.getOperands();
        RexNodeBase exprConverter = RexNodeConverter.rexConstraints(inputs,operands.get(0),z3Context);
        Expr compareExpr = exprConverter.getOutputNull();
        this.assignConstraints.addAll(exprConverter.getAssignConstrains());
        List<BoolExpr> valueConstrains = new ArrayList<>();
        List<BoolExpr> nullValues = new ArrayList<>();
        List<BoolExpr> nullSymbol = new ArrayList<>();
        for(int i=1;i<operands.size();i++){
            RexNode value = operands.get(i);
            RexNodeBase valueConverter = RexNodeConverter.rexConstraints(inputs,value,z3Context);
            Expr valueExpr = valueConverter.getOutputValue();
            BoolExpr result = z3Context.mkEq(compareExpr,valueExpr);
            BoolExpr symbolicNull = valueConverter.getOutputNull();
            valueConstrains.add(result);
            nullValues.add(z3Context.mkOr(symbolicNull,z3Context.mkNot(result)));
            nullSymbol.add(symbolicNull);
        }
        Expr outputValue = z3Utility.mkOr(valueConstrains,z3Context).simplify();
        BoolExpr finalNull = (BoolExpr) z3Context.mkAnd(z3Utility.mkAnd(nullValues,z3Context),z3Utility.mkOr(nullSymbol,z3Context)).simplify();
        BoolExpr outputNull = z3Context.mkOr(exprConverter.getOutputNull(),finalNull);
        this.output = new SymbolicColumn(outputValue,outputNull,z3Context);
    }
}
