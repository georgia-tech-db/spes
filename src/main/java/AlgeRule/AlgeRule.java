package AlgeRule;

import AlgeNode.AlgeNode;

import java.util.ArrayList;
import java.util.List;

public class AlgeRule {
    public static AlgeNode normalize(AlgeNode node){
        return pushDownNormalize(pullUpNormalize(node));
    }

    private static AlgeNode pullUpNormalize(AlgeNode node){
        List<Class> pullUpSimplifyRules = new ArrayList<>();
        pullUpSimplifyRules.add(SPJ2Empty.class);
        pullUpSimplifyRules.add(CleanEmpty.class);
        List<AlgeNode> newInputs = new ArrayList<>();
        for (AlgeNode input : node.getInputs()) {
           newInputs.add(pullUpNormalize(input));
        }
        node.setInputs(newInputs);
        node = simplifyBaseRules(node,pullUpSimplifyRules);
        return node;
    }

    private static AlgeNode simplifyBaseRules(AlgeNode node,List<Class> simplifyRules) {
        boolean canBeRewrite = true;
        while (canBeRewrite) {
            canBeRewrite = false;
            for (Class rule : simplifyRules) {
                AlgeRuleBase rulePerform = new DummyRule(node);
                try {
                    rulePerform = (AlgeRuleBase) rule.getDeclaredConstructor(AlgeNode.class).newInstance(node);
                } catch (Exception e) {
                    System.out.println(e);
                }
                if (rulePerform.preCondition()) {
                    node = rulePerform.transformation();
                    node.enableRewrite();
                    canBeRewrite = true;
                }
            }
        }
        return node;
    }

    private static AlgeNode pushDownNormalize(AlgeNode node){
        List<Class> pushDownSimplifyRules = new ArrayList<>();
        pushDownSimplifyRules.add(AggregateMerge.class);
        pushDownSimplifyRules.add(ConditionPushAgg.class);
        pushDownSimplifyRules.add(JoinToProject.class);
        node = simplifyBaseRules(node,pushDownSimplifyRules);
        List<AlgeNode> newInputs = new ArrayList<>();
        for (AlgeNode input : node.getInputs()) {
            newInputs.add(pushDownNormalize(input));
        }
        node.setInputs(newInputs);
        return node;
    }
}
