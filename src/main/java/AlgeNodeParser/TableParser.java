package AlgeNodeParser;

import AlgeNode.AlgeNode;
import AlgeNode.TableNode;
import AlgeNode.SPJNode;
import com.microsoft.z3.Context;
import org.apache.calcite.adapter.enumerable.EnumerableTableScan;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TableParser extends AlgeNodeParser{
    public AlgeNode constructRelNode(RelNode input, Context z3Context){
        EnumerableTableScan tableScan = (EnumerableTableScan) input;
        RelOptTable table = tableScan.getTable();
        String tableName = table.getQualifiedName().get(0);
        List<RelDataTypeField> columns = tableScan.getRowType().getFieldList();
        ArrayList<RelDataType> columnTypes = new ArrayList<>();
        for (RelDataTypeField column:columns){
            columnTypes.add(column.getType());
        }
        TableNode tableNode = new TableNode(tableName,columnTypes,z3Context);
        return wrapBySPJ(tableNode,z3Context);

    }

    private SPJNode wrapBySPJ (TableNode tableNode, Context z3Context){
        Set<RexNode> emptyCondition = new HashSet<>();
        List<AlgeNode> inputs = new ArrayList<AlgeNode>();
        inputs.add(tableNode);
        return (new SPJNode(tableNode.getOutputExpr(),emptyCondition,inputs,z3Context));
    }
}
