package SymbolicRexNode;

import RexNodeHelper.NotIn;
import com.microsoft.z3.Context;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import java.util.Arrays;
import java.util.List;

public class RexNodeConverter {
    static public List<SqlKind> logicSQL = Arrays.asList(
            SqlKind.AND, SqlKind.OR, SqlKind.NOT, SqlKind.IN);
    static public List<SqlKind> isNullSQL = Arrays.asList(
            SqlKind.IS_NULL, SqlKind.IS_NOT_NULL);
    static public List<SqlKind> arithmeticCompareSQL = Arrays.asList(
            SqlKind.LESS_THAN, SqlKind.LESS_THAN_OR_EQUAL,
            SqlKind.GREATER_THAN, SqlKind.GREATER_THAN_OR_EQUAL,
            SqlKind.EQUALS, SqlKind.NOT_EQUALS);
    static public List<SqlKind> arithmeticSQL = Arrays.asList(
            SqlKind.PLUS, SqlKind.MINUS, SqlKind.TIMES, SqlKind.DIVIDE);

    public static RexNodeBase rexConstraints(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        if(node instanceof RexLiteral){
            return new Constant(inputs,node,z3Context);
        } else if(node instanceof RexCall){
            return rexCallConstraints(inputs,node,z3Context);
        } else if(node instanceof RexInputRef){
            return new RexInputRefConstraints(inputs,node,z3Context);
        } else {
            return null;
        }
    }

    private static RexNodeBase rexCallConstraints(List<SymbolicColumn> inputs, RexNode node, Context z3Context) {
        RexCall callNode = (RexCall) node;
        if (callNode instanceof NotIn){
           return  (new RexNotIn(inputs,node,z3Context));
        }
        if (callNode.isA(SqlKind.CASE)) {
            return (new CaseNode(inputs, node, z3Context));
        } else if (callNode.isA(SqlKind.OTHER_FUNCTION)) {
            return (new UserDeFun(inputs, node, z3Context));
        } else if (callNode.isA(logicSQL)) {
            return (new BoolPredicate(inputs, node, z3Context));
        } else if (callNode.isA(isNullSQL)) {
            return (new NullPredicate(inputs, node, z3Context));
        } else if (callNode.isA(arithmeticCompareSQL)) {
            return (new ArithmeticPredicate(inputs, node, z3Context));
        } else if (callNode.isA(arithmeticSQL)) {
            return (new ArithmeticExpr(inputs, node, z3Context));
        } else if (callNode.isA(SqlKind.CAST)) {
            RexNode operand = callNode.getOperands().get(0);
            return rexConstraints(inputs, operand, z3Context);
        } else if (callNode.isA(SqlKind.IS_TRUE)) {
            RexNode operand = callNode.getOperands().get(0);
            return rexConstraints(inputs, operand, z3Context);
        } else {
            System.out.println("`rexCallConstraints` does not handle this type of call node: " + callNode.getClass());
            return new DumpRexNode(inputs, node, z3Context);
        }
    }
}
