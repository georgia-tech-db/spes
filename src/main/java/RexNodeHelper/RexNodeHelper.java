package RexNodeHelper;

import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RexNodeHelper {
    public static final JavaTypeFactory typeFactory = new JavaTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
    /**
     * Recursively descend over the node, applying the input function to each sub-node in a call.
     * @param f The function to apply
     * @param node The input node
     * @return The new node, resulting from applying the function to the original node.
     */
    private static RexNode mapNode(Function<RexNode, RexNode> f, RexNode node) {
        if (node instanceof RexCall) {
            if (node instanceof NotIn){
                NotIn notIn = (NotIn) node;
                List<RexNode> columns = notIn.getColumns().stream()
                        .map((RexNode operand) -> mapNode(f, operand))
                        .collect(Collectors.toList());
                return (new NotIn(notIn.getSubQuery(),columns));
            }
            RexCall rexCall = (RexCall) node;
            List<RexNode> operands = rexCall.getOperands().stream()
                    .map((RexNode operand) -> mapNode(f, operand))
                    .collect(Collectors.toList());
            return rexCall.clone(rexCall.getType(), operands);
        } else {
            return f.apply(node);
        }
    }

    private static RexNode unhandled(RexNode node, String procedureName) {
        System.out.println("This rex node is not handled in `" + procedureName + "`: " + node.getClass().toString());
        return node;
    }

    public static RexNode substitute(RexNode input, List<RexNode> outputExpr) {
        return mapNode((RexNode node) -> {
                if (node instanceof RexInputRef) {
                    RexInputRef ref = (RexInputRef) node;
                    return outputExpr.get(ref.getIndex());
                } else if (node instanceof RexLiteral) {
                    return node;
                } else {
                    return unhandled(node, "substitute");
                }
            }, input);
    }

    public static RexNode substitute(RexNode input, Map<Integer, Integer> indexMap) {
        return mapNode((RexNode node) -> {
            if (node instanceof RexInputRef) {
                RexInputRef ref = (RexInputRef) node;
                int index = ref.getIndex();
                if (indexMap.containsKey(index)){
                    return new RexInputRef(indexMap.get(index), ref.getType());
                }else{
                    return ref;
                }
            } else if (node instanceof RexLiteral) {
                return node;
            } else {
                return unhandled(node, "substitute");
            }
        }, input);
    }

    public static RexNode addOffSize(RexNode input,int offSize){
        return mapNode((RexNode node) -> {
            if (node instanceof RexInputRef) {
                RexInputRef ref = (RexInputRef) node;
                int nexIndex = ref.getIndex() + offSize;
                RexInputRef newRef = new RexInputRef(nexIndex,ref.getType());
                return newRef;
            } else if (node instanceof RexLiteral) {
                return node;
            } else {
                return unhandled(node, "addTableIndex");
            }
        }, input);
    }

    public static RexNode minusOffSize(RexNode input,int offSize){
        return mapNode((RexNode node) -> {
            if (node instanceof RexInputRef) {
                RexInputRef ref = (RexInputRef) node;
                int nexIndex = ref.getIndex() - offSize;
                RexInputRef newRef = new RexInputRef(nexIndex,ref.getType());
                return newRef;
            } else if (node instanceof RexLiteral) {
                return node;
            } else {
                return unhandled(node, "addTableIndex");
            }
        }, input);
    }

    public static Set<RexNode> collectVariables(RexNode input){
        Set<RexNode> result = new HashSet<>();
        collectVariables(input,result);
        return result;
    }

    public static void collectVariables(RexNode input, Set<RexNode> result){
        if(input instanceof RexInputRef){
           result.add(input);
        } else if(input instanceof RexCall) {
            ((RexCall) input).getOperands().forEach((RexNode operand) -> collectVariables(operand, result));
        }
    }
}