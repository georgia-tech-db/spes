package AlgeNodeParser;

import AlgeNode.AlgeNode;
import AlgeNode.UnionNode;
import AlgeNode.SPJNode;
import RexNodeHelper.RexNodeHelper;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;

public class ProjectParser extends AlgeNodeParser{
    public AlgeNode constructRelNode(RelNode input, Context z3Context){
        LogicalProject project = (LogicalProject) input;
        AlgeNode inputNode = AlgeNodeParserPair.constructAlgeNode(project.getInput(),z3Context);
        if (inputNode instanceof UnionNode){
            updateUnion((UnionNode) inputNode,project.getProjects());
            return inputNode;
        }
        if (inputNode instanceof SPJNode){
            updateSPJ(inputNode,project.getProjects());
            return inputNode;
        }
        else {
            System.out.println("error in project parser:"+inputNode.toString());
            return inputNode;
        }
    }
    private void updateSPJ (AlgeNode spjNode, List<RexNode> columns){
        updateOutputExprs(spjNode,columns);
    }
    private void updateUnion (UnionNode unionNode,List<RexNode> columns){
        for (AlgeNode input:unionNode.getInputs()){
            updateOutputExprs(input,columns);
        }
    }
    private void updateOutputExprs(AlgeNode inputNode,List<RexNode> columns){
        List<RexNode> inputExprs = inputNode.getOutputExpr();
        List<RexNode> newOutputExpr = new ArrayList<>();
        for (RexNode column:columns){
            newOutputExpr.add(RexNodeHelper.substitute(column,inputExprs));
        }
        inputNode.setOutputExpr(newOutputExpr);
    }
}
