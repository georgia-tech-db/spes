package AlgeNode;
import SymbolicRexNode.SymbolicColumn;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;

import java.util.*;

public class TableNode extends AlgeNode{
    String name;

    public TableNode(String name, List<RelDataType> columnTypes, Context z3Context) {
        List<RexNode> columns = constructColumns(columnTypes);
        List<AlgeNode> emptyInputTables = new ArrayList<>();
        Set<RexNode> emptyFilterCondition = new HashSet<>();
        setBasicFields(z3Context, columnTypes, emptyInputTables, columns, emptyFilterCondition);
        this.name = name;
    }

    private List<RexNode> constructColumns (List<RelDataType> columnTypes){
        List<RexNode> columns = new ArrayList<>();
        int count = 0;
        for (RelDataType columnType:columnTypes){
            RexInputRef expr = new RexInputRef(count, columnType);
            columns.add(expr);
            count++;
        }
        return columns;
    }

    @Override
    public String toString() {
        return "Table: " + name + "\n" + super.toString();
    }

    public String getName(){
        return this.name;
    }

    public boolean constructQPSR(AlgeNode node){
        if (node instanceof TableNode){
            TableNode tableNode = (TableNode) node;
            if (this.isCartesianEq(tableNode)){
                this.constructQPSR(tableNode);
                this.variableConstraintTrue();
                tableNode.variableConstraintTrue();
                return true;
            }
        }
        return false;
    }

    public boolean isCartesianEq(TableNode tableNode) {
        if (this.name.equals(tableNode.getName())) {
            return true;
        }
        return false;
    }


    private void constructQPSR(TableNode node){
        List<SymbolicColumn> symbolicTuple = SymbolicColumn.constructSymbolicTuple(inputTypes,z3Context);
        this.setSymbolicColumns(symbolicTuple);
        node.setSymbolicColumns(symbolicTuple);
    }

    public void setSymbolicColumns(List<SymbolicColumn> symbolicTuple){
        this.symbolicColumns = symbolicTuple;
    }

    public void variableConstraintTrue () {
        this.variableConstraints = z3Context.mkTrue();
    }

    public AlgeNode clone () {
        List<RelDataType> columnsTypes = new ArrayList<>();
        for (RexNode expr : this.outputExpr ){
            columnsTypes.add(expr.getType());
        }
        return (new TableNode(this.name,columnsTypes,z3Context));
    }

}
