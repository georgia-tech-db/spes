package SymbolicRexNode;

import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;

public class RexNodeUtility {
    static public Sort convertRexNodeSort(Context z3Context, RexNode node){
        SqlTypeName type = node.getType().getSqlTypeName();
        return getSortBasedOnSqlType(z3Context, type);
    }

    static public Sort getSortBasedOnSqlType(Context z3Context, SqlTypeName type) {
        if (SqlTypeName.APPROX_TYPES.contains(type)) {
            return z3Context.mkRealSort();
        } else if(SqlTypeName.INT_TYPES.contains(type)) {
            return z3Context.mkIntSort();
        } else if(type.equals(SqlTypeName.DECIMAL)) {
            return z3Context.mkRealSort();
        } else if(SqlTypeName.BOOLEAN_TYPES.contains(type)) {
            return z3Context.mkBoolSort();
        } else if(type.equals(SqlTypeName.CHAR)) {
            return z3Context.mkIntSort();
        } else if(type.equals(SqlTypeName.VARCHAR)) {
            return z3Context.mkIntSort();
        } else {
            return z3Context.mkIntSort();
        }
    }

    static public void reset(){
        RexNotIn.reset();
        UserDeFun.reset1();
    }
}
