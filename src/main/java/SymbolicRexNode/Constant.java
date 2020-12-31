package SymbolicRexNode;

import Z3Helper.z3Utility;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.NlsString;

import java.math.BigDecimal;
import java.util.List;

public class Constant extends RexNodeBase {
    public Constant(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        buildExpr(node);

    }
    private void buildExpr(RexNode node){
        RexLiteral constant = (RexLiteral) node;
        SqlTypeName type = constant.getTypeName();
        if(type.equals(SqlTypeName.NULL)){
            this.output = new SymbolicColumn(z3Utility.mkDumpValue(constant.getType(),z3Context),z3Context.mkTrue(),z3Context);
        }else{
            this.output = new SymbolicColumn(convertRexLiteralNotNull(constant),z3Context.mkFalse(),z3Context);
        }
    }
    private Expr convertRexLiteralNotNull(RexLiteral constant){
        SqlTypeName type = constant.getTypeName();
        if(SqlTypeName.APPROX_TYPES.contains(type)){
            return convertApproxType(constant);
        }
        if(SqlTypeName.INT_TYPES.contains(type)){
            BigDecimal value=(BigDecimal)constant.getValue();
            int intConstant = value.intValue();
            return z3Context.mkInt(intConstant);
        }
        if(type.equals(SqlTypeName.DECIMAL)){
            BigDecimal value=(BigDecimal)constant.getValue();
            return z3Context.mkReal(value.toString());
        }
        if(SqlTypeName.BOOLEAN_TYPES.contains(type)){
            Boolean value = (Boolean)constant.getValue();
            if(value == null){
                return z3Utility.getDumpVariable(z3Context.mkBoolSort(),z3Context);
            }else{
                return z3Context.mkBool(value);
            }
        }
        if(type.equals(SqlTypeName.CHAR)) {
            NlsString value = (NlsString) constant.getValue();
            return z3Context.mkInt(value.getValue().hashCode());
        }
        if(type.equals(SqlTypeName.VARCHAR)){
            NlsString value = (NlsString) constant.getValue();
            return z3Context.mkInt(value.getValue().hashCode());
        }
        return z3Context.mkInt(0);

    }
    private Expr convertApproxType(RexLiteral constant){
        SqlTypeName type = constant.getTypeName();
        if(SqlTypeName.APPROX_TYPES.contains(type)){
            if(constant.getValue() instanceof Double){
                Double value = (Double) constant.getValue();
                return z3Context.mkReal(value.toString());
            }
            if(constant.getValue() instanceof Float){
                Float value = (Float) constant.getValue();
                return z3Context.mkReal(value.toString());
            }
        }
        BigDecimal value = (BigDecimal)constant.getValue();
        return z3Context.mkReal(value.toString());
    }
}
