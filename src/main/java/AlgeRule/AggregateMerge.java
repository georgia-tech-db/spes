package AlgeRule;

import AlgeNode.AlgeNode;
import AlgeNode.AggNode;
import AlgeNode.SPJNode;
import AlgeNode.UnionNode;
import RexNodeHelper.RexNodeHelper;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import java.util.*;

public class AggregateMerge extends AlgeRuleBase{
    private List<AggregateCall> newCalls;
    public AggregateMerge(AlgeNode input){
        this.input = input;
    }
    public boolean preCondition() {
        if(this.input instanceof AggNode){
            AggNode aggNode = (AggNode) this.input;
            AlgeNode subInput = aggNode.getInput();
            if(subInput instanceof SPJNode){
                SPJNode parent = (SPJNode) subInput;
                if (parent.getInputs().size() == 1) {
                    AlgeNode child = parent.getInputs().get(0);
                    if (child instanceof AggNode){
                        return canMerge(aggNode,(AggNode) child,parent);
                    }
                }
            }
        }
        return false;
    }

    private boolean canMerge (AggNode input, AggNode subInput, SPJNode parent) {
        if (GroupByEntail(input,subInput,parent.getOutputExpr())){
            if (conditionsNoAgg(subInput,parent.getConditions())){
                return isMergeAggCalls(input,subInput,parent.getOutputExpr());
            }
        }
        return false;
    }

    private boolean isMergeAggCalls(AggNode input,AggNode subInput,List<RexNode> outputExprs){
        this.newCalls = new ArrayList<>();
        for(AggregateCall aggregateCall:input.getAggregateCallList()){
                if(!isMergeAggCall(aggregateCall,subInput,outputExprs)){
                    return false;
                }
        }
        return true;
    }

    private AggregateCall setDistinct(AggregateCall call){
        return AggregateCall.create(call.getAggregation(),true,call.isApproximate(),call.getArgList(),call.filterArg,call.getCollation(),call.getType(),call.getName());
    }

    private boolean isMergeAggCall(AggregateCall call,AggNode subInput, List<RexNode> outputExprs){
         if(isAppendableAgg(call)){
             //TODO: currently only support aggCall take one operands
             Integer operand = call.getArgList().get(0);
             RexNode inputColumn = outputExprs.get(operand);
             int groupBySize = subInput.getGroupByList().size();
             if(inputColumn instanceof RexInputRef){
                 int index = ((RexInputRef)inputColumn).getIndex();
                 if(index >= groupBySize){
                     AggregateCall inputAggCall = subInput.getAggregateCallList().get(index - groupBySize);
                     if(inputAggCall.getAggregation().getKind().equals(call.getAggregation().getKind())){
                         if ((!inputAggCall.isDistinct()) && (!inputAggCall.isApproximate()) && (!call.isDistinct()) && (!call.isApproximate())){
                             this.newCalls.add(call);
                             return true;
                         }
                     }else{
                         return false;
                     }
                 }else{
                     if((index==0) && (groupBySize == 1)){
                         this.newCalls.add(setDistinct(call));
                     }else{
                         this.newCalls.add(call);
                     }
                     return true;
                 }
             }
         }
         return false;
    }
    private boolean isAppendableAgg(AggregateCall call){
        for(SqlKind sqlKind : AggNode.appendableAgg){
            if(call.getAggregation().getKind().equals(sqlKind)){
                return true;
            }
        }
        return false;
    }

    private boolean GroupByEntail (AggNode input, AggNode subInput, List<RexNode> outputExprs){
        List<RexNode> subInputGroups = subInput.getGroupByVariables();
        for (int index: input.getGroupByList()){
            RexNode outputExpr = outputExprs.get(index);
            if(!subInputGroups.containsAll(RexNodeHelper.collectVariables(outputExpr))){
                return false;
            }
        }
        return true;
    }

    private boolean conditionsNoAgg(AggNode input,Set<RexNode> conditions){
        List<RexNode> groupByVariables = input.getGroupByVariables();
        for(RexNode condition:conditions){
            if(!groupByVariables.containsAll(RexNodeHelper.collectVariables(condition))){
                return false;
            }
        }
        return true;
    }

    public AlgeNode transformation(){
        AggNode aggNode = (AggNode) input;
        SPJNode parent = (SPJNode) aggNode.getInput();
        AggNode child = (AggNode) parent.getInputs().get(0);
        AlgeNode childInput = child.getInput();
        if (childInput instanceof SPJNode){
            normalizeSPJNode((SPJNode) childInput,child);
            updateSPJCode((SPJNode)childInput,parent.getOutputExpr(),parent.getConditions());
        }
        if (childInput instanceof UnionNode){
            normalizeUnionNode((UnionNode) childInput,child);
            updateUnionNode((UnionNode)childInput,parent.getOutputExpr(),parent.getConditions());
        }
        List<AlgeNode> newInput = new ArrayList<>();
        newInput.add(childInput);
        aggNode.setInputs(newInput);
        aggNode.setAggregateCallList(this.newCalls);
        return aggNode;
    }

    private void normalizeSPJNode (SPJNode input, AggNode aggNode){
        List<RexNode> newOutputExpr = new ArrayList<>();
        for (Integer columnIndex:aggNode.getGroupByList()){
            newOutputExpr.add(input.getOutputExpr().get(columnIndex));
        }
        for (AggregateCall aggregateCall : aggNode.getAggregateCallList()){
            List<Integer> argList = aggregateCall.getArgList();
            if (argList.size() == 1){
                newOutputExpr.add(input.getOutputExpr().get(argList.get(0)));
            }
        }
        input.setOutputExpr(newOutputExpr);
    }

    private void normalizeUnionNode (UnionNode input, AggNode aggNode){
        for (AlgeNode spj : input.getInputs()){
            normalizeSPJNode((SPJNode) spj,aggNode);
        }
    }

   private void updateSPJCode (SPJNode input, List<RexNode> outputExprs, Set<RexNode> conditions){
        List<RexNode> newOutputExprs = new ArrayList<>();
        for (RexNode oldExpr : outputExprs){
            newOutputExprs.add(RexNodeHelper.substitute(oldExpr,input.getOutputExpr()));
        }
        input.setOutputExpr(newOutputExprs);
        for (RexNode condition: conditions){
            input.addCondition(RexNodeHelper.substitute(condition,input.getOutputExpr()));
        }
   }

   private void updateUnionNode (UnionNode input, List<RexNode> outputExprs, Set<RexNode> conditions){
        for (AlgeNode spj : input.getInputs()){
            updateSPJCode((SPJNode) spj, outputExprs,conditions);
        }
   }
}
