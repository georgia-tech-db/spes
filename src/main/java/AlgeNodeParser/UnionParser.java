package AlgeNodeParser;

import AlgeNode.AlgeNode;
import AlgeNode.UnionNode;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalUnion;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import java.util.ArrayList;
import java.util.List;

public class UnionParser extends AlgeNodeParser{
    public AlgeNode constructRelNode(RelNode input, Context z3Context){
        LogicalUnion logicalUnion = (LogicalUnion) input;
        List<AlgeNode> inputs = new ArrayList<AlgeNode>();
        for(int i=0;i<logicalUnion.getInputs().size();i++){
            AlgeNode newNode = AlgeNodeParserPair.constructAlgeNode(logicalUnion.getInput(i),z3Context);
            inputs.addAll(normalizeNodes(newNode));
        }
        List<RelDataType> inputTypes = new ArrayList<>();
        for(RelDataTypeField field:logicalUnion.getRowType().getFieldList()){
            inputTypes.add(field.getType());
        }
        UnionNode unionNode = new UnionNode(inputs,z3Context,inputTypes);
        if (logicalUnion.all){
            return unionNode;
        }else{
            return AggParser.distinctToAgg(unionNode);
        }
    }
    public List<AlgeNode> normalizeNodes (AlgeNode input){
        List<AlgeNode> result = new ArrayList<>();
        if (input instanceof UnionNode){
            result.addAll(input.getInputs());
        }else{
            result.add(input);
        }
        return result;
    }
}
