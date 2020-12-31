package SymbolicRexNode;

import AlgeNode.AlgeNode;
import AlgeRule.AlgeRule;
import RexNodeHelper.NotIn;
import Z3Helper.z3Utility;
import com.microsoft.z3.Context;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;

public class RexNotIn extends RexNodeBase {
    static List<SymbolicRexNotIn> previousNotIns = new ArrayList<>();
    static public void reset () {
        previousNotIns = new ArrayList<>();
    }

    class SymbolicRexNotIn {
        private SymbolicColumn result;
        private List<SymbolicColumn> symbolicColumns;
        private AlgeNode subquery;

        public SymbolicRexNotIn (SymbolicColumn result,List<SymbolicColumn> symbolicColumns,AlgeNode subquery){
            this.result = result;
            this.symbolicColumns = symbolicColumns;
            this.subquery = subquery;
        }

        public boolean eq (List<SymbolicColumn> symbolicColumns, AlgeNode subquery){
            boolean result = z3Utility.symbolicOutputEqual(z3Context.mkTrue(),symbolicColumns,this.symbolicColumns,z3Context);
            if (result){
                if (subquery.isEq(this.subquery)){
                    return true;
                }
            }
            return false;
        }

        public SymbolicColumn getResult() {
            return result;
        }
    }

    public RexNotIn(List<SymbolicColumn> inputs, RexNode node, Context z3Context) {
        super(inputs,node,z3Context);
        buildExpr((NotIn) node);
    }
    private void buildExpr(NotIn node) {
        List<RexNode> columns = node.getColumns();
        List<SymbolicColumn> compareColumns = new ArrayList<>();
        for (RexNode column : columns) {
            RexNodeBase columnConverter = RexNodeConverter.rexConstraints(inputs,column,z3Context);
            compareColumns.add(columnConverter.getOutput());
        }
        boolean findMatch = false;
        for (SymbolicRexNotIn previousNotIn : previousNotIns) {
            if (previousNotIn.eq(compareColumns,node.getSubQuery())){
                this.output = previousNotIn.getResult();
                findMatch = true;
                break;
            }
        }
        if (!findMatch){
            this.output = SymbolicColumn.mkNewSymbolicColumn(z3Context,node.getType());
            SymbolicRexNotIn newSymbolicNotIn = new SymbolicRexNotIn(this.output,compareColumns,node.getSubQuery());
            previousNotIns.add(newSymbolicNotIn);
        }
    }
}
