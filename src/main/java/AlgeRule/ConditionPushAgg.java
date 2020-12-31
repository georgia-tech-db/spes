package AlgeRule;

import AlgeNode.AlgeNode;
import AlgeNode.SPJNode;
import AlgeNode.UnionNode;
import AlgeNode.AggNode;
import AlgeNodeParser.FilterParser;
import RexNodeHelper.RexNodeHelper;
import org.apache.calcite.rex.RexNode;

import java.util.*;

public class ConditionPushAgg extends AlgeRuleBase{
    private Set<pushDownCondition> pushDownConditions;
    private Set<RexNode> nonPushedConditions;
    class pushDownCondition {
        private int tableIndex;
        private RexNode condition;
        public pushDownCondition (int tableIndex,RexNode condition){
            this.tableIndex = tableIndex;
            this.condition = condition;
        }

        public int getTableIndex() {
            return tableIndex;
        }

        public RexNode getCondition() {
            return condition;
        }
    }
    public ConditionPushAgg (AlgeNode input){
        this.input = input;
    }
    public boolean preCondition(){
        this.pushDownConditions = new HashSet<>();
        this.nonPushedConditions = new HashSet<>();
        boolean result = false;
        if (this.input instanceof SPJNode) {
            SPJNode spjNode = (SPJNode) this.input;
            Set<RexNode> conditions = spjNode.getConditions();
            int offSize = 0;
            int tableIndex = 0;
            for (AlgeNode input : spjNode.getInputs()) {
                if (input instanceof AggNode) {
                    AggNode aggNode = (AggNode) input;
                    for (RexNode condition : conditions) {
                        if (isPushDown(aggNode, condition,offSize)) {
                            result = true;
                            RexNode newCondition = offsetCondition(condition, offSize);
                            this.pushDownConditions.add(new pushDownCondition(tableIndex, newCondition));
                        }else{
                            this.nonPushedConditions.add(condition);
                        }
                    }
                }
                offSize = offSize + input.getOutputExpr().size();
                tableIndex++;
            }
        }
        return result;
    }

    private RexNode offsetCondition (RexNode condition, int offsize) {
       return RexNodeHelper.minusOffSize(condition,offsize);
    }

    private boolean isPushDown(AggNode aggNode, RexNode condition,int offsize){
        List<RexNode> groupByVariables = aggNode.getGroupByVariables();
        List<RexNode> newGroupByVariables = new ArrayList<>();
        for (RexNode groupByVariable : groupByVariables){
            newGroupByVariables.add(RexNodeHelper.addOffSize(groupByVariable,offsize));
        }
        Set<RexNode> variables = new HashSet<>();
        RexNodeHelper.collectVariables(condition,variables);
        return groupByVariables.containsAll(variables);
    }

    public AlgeNode transformation () {
        for (pushDownCondition c : pushDownConditions){
            AggNode aggNode = (AggNode) input.getInputs().get(c.getTableIndex());
            pushDown(aggNode,c.getCondition());
        }
        this.input.setConditions(this.nonPushedConditions);
        return input;
    }

    private AlgeNode pushDown (AggNode aggNode, RexNode condition){
        AlgeNode inputNode = aggNode.getInput();
        if (inputNode instanceof UnionNode) {
            UnionNode unionNode = (UnionNode) inputNode;
            for (AlgeNode input : unionNode.getInputs()){
                RexNode newCondition = RexNodeHelper.substitute(condition,input.getOutputExpr());
                input.addConditions(FilterParser.conjunctiveForm(newCondition));
            }
        }
        if (inputNode instanceof SPJNode) {
            RexNode newCondition = RexNodeHelper.substitute(condition,inputNode.getOutputExpr());
            inputNode.addConditions(FilterParser.conjunctiveForm(newCondition));
        }
        return aggNode;
    }
}
