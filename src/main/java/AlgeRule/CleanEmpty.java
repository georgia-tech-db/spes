package AlgeRule;

import AlgeNode.AlgeNode;
import AlgeNode.UnionNode;
import AlgeNode.AggNode;
import AlgeNode.SPJNode;
import AlgeNode.EmptyNode;

import java.util.ArrayList;
import java.util.List;

public class CleanEmpty extends AlgeRuleBase{
    private List<AlgeNode> unionInputs;

    public CleanEmpty(AlgeNode input) { this.input = input; }

    @Override
    public boolean preCondition() {
        if (input instanceof UnionNode) {
            unionInputs = new ArrayList<>();
            for (AlgeNode input : input.getInputs()){
                if (!(input instanceof EmptyNode)) {
                    unionInputs.add(input);
                }
            }
            return (unionInputs.size()==1)||(unionInputs.size() != input.getInputs().size());
        }
        if (input instanceof AggNode){
            AggNode aggNode = (AggNode) input;
            if (aggNode.getGroupByList().isEmpty()) {
                if (aggNode.getInput() instanceof EmptyNode) {
                    return true;
                }
            }
        }
        if (input instanceof SPJNode){
            for (AlgeNode input : input.getInputs()){
                if (input instanceof EmptyNode){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AlgeNode transformation() {
        if (this.input instanceof UnionNode){
            if (this.unionInputs.isEmpty()){
                return (new EmptyNode(this.input.getZ3Context()));
            }
            if (this.unionInputs.size() == 1) {
                return this.unionInputs.get(0);
            }else{
                this.input.setInputs(this.unionInputs);
                return (this.input);
            }
        }
        if (this.input instanceof SPJNode || this.input instanceof AggNode){
            return (new EmptyNode(this.input.getZ3Context()));
        }
        return this.input;
    }
}
